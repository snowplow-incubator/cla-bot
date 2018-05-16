package clabot

import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect.IO
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model._
import com.typesafe.config.ConfigFactory
import fs2.{Sink, Stream}
import io.circe.config.syntax._
import io.circe.generic.auto._
import io.circe.parser._

import config._
import model._

object Main {
  private val client = AmazonSQSAsyncClientBuilder.defaultClient()
  private val yesMessage = "Thanks for signing the CLA!"
  private val yesLabel = "cla:yes"
  private val noMessage = "Please sign the CLA: https://github.com/snowplow/snowplow/wiki/CLA.\n" +
    "Once you signed it, ping the bot."
  private val noLabel = "cla:no"

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.load()

    conf.as[ClaBotConfig] match {
      case Left(e) =>
        System.err.println(s"configuration error: $e")
        System.exit(1)
      case Right(c) =>
        main(c)
    }
  }

  def main(conf: ClaBotConfig): Unit = {
    val gsheets = new GSheets(conf.gsheets.toCredentials)
    val github = new Github(conf.github.token)

    def loggingSink[A]: Sink[IO, A] = _.evalMap(a => IO(println(a.toString)))

    println("Starting...")

    // get pr event stream
    val rmr = new ReceiveMessageRequest(conf.aws.sqsQueueUrl)
      .withMaxNumberOfMessages(1)
      .withWaitTimeSeconds(10)
    val dmr = (m: Message) => new DeleteMessageRequest(conf.aws.sqsQueueUrl, m.getReceiptHandle())
    val sqsStream: Stream[IO, Message] = sqs
      .messageStream(client, rmr)
      //.observe(loggingSink)
      .observe(sqs.deleteSink(client, dmr))

    val pullRequestStream: Stream[IO, PR] = sqsStream
      .map(m => decode[IssueCommentEvent](m.getBody))
      .collect { case Right(ic) => ic }
      .map { event => (
        event.repository.full_name,
        event.sender.login,
        event.issue.number,
        event.comment.body
      ) }
      .filter(_._2 != conf.github.botName)
      .collect { case (fullName, login, n, body) =>
        val Array(owner, repoName) = fullName.split("/")
        PR(owner, repoName, login, n, Some(body))
      }
      .observe(loggingSink)

    // filter comments which have been made by the bot itself
    val notBotStream: Stream[IO, PR] = pullRequestStream.filter(_.creator != conf.github.botName)

    // filter comments that dont contain a bot ping
    val hasBotPing: Stream[IO, PR] = pullRequestStream.filter(pr => pr.body.isDefined && pr.body.get.contains(s"@${conf.github.botName}"))

    // filter prs which already have cla:yes label
    val claNotSignedStream: Stream[IO, PR] = hasBotPing
      .evalMap { pr =>
        github.listLabels(pr.owner, pr.repo, pr.number.toString)
          .map((pr, _))
      }
      .collect { case (pr, labels) if !labels.contains(yesLabel) => pr }
      .observe(loggingSink)

    // filter events that are issues but not prs
    val prStream: Stream[IO, PR] = claNotSignedStream
      .evalMap { pr =>
        github.getPull(pr.owner, pr.repo, pr.number.toString)
          .map((pr, _))
      }
      .collect { case (pr, response) if response.isSuccess => pr }
      .observe(loggingSink)

    // filter pr creator who are in the org
    val outsideContributorStream: Stream[IO, PR] = prStream
      .evalMap { pr =>
        github.listMembers(conf.github.org)
          .map((pr, _))
      }
      .collect { case (pr, orgMembers) if !orgMembers.contains(pr.creator) => pr }
      .observe(loggingSink)

    // check the google sheet
    val hasSignedClaStream: Stream[IO, (PR, Boolean)] = outsideContributorStream
      .evalMap { pr =>
        gsheets
          .get(conf.gsheets.spreadsheetId, conf.gsheets.sheetName, conf.gsheets.column)
          .map((pr, _))
      }
      .collect { case (pr, Right(signers)) => (pr, signers) }
      .map { case (pr, signers) => (pr, signers.contains(pr.creator))}
      .observe(loggingSink)

    // post a message saying yes / no
    val messageSink: Sink[IO, (PR, Boolean)] = _
      .evalMap {
        case (pr, true) => sinkIO(github, pr, yesMessage, noLabel, yesLabel)
        case (pr, false) => sinkIO(github, pr, noMessage, yesLabel, noLabel)
      }

    hasSignedClaStream
      .observe(messageSink)
      .compile
      .drain
      .unsafeRunSync()
  }

  def sinkIO(github: Github, pr: PR, msg: String, oldLabel: String, newLabel: String): IO[Unit] = for {
    _ <- github.postMessage(pr.owner, pr.repo, pr.number.toString, msg)
    _ <- github.deleteLabel(pr.owner, pr.repo, pr.number.toString, oldLabel)
    _ <- github.addLabel(pr.owner, pr.repo, pr.number.toString, newLabel)
  } yield ()
}
