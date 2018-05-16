package clabot

import cats.effect.IO
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model._
import com.typesafe.config.ConfigFactory
import fs2.Stream
import io.circe.config.syntax._
import io.circe.generic.auto._
import io.circe.parser._

import config._
import model._

object Main {
  private val client = AmazonSQSAsyncClientBuilder.defaultClient()

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

    // get pr event stream
    val rmr = new ReceiveMessageRequest(conf.aws.sqsQueueUrl)
      .withMaxNumberOfMessages(1)
      .withWaitTimeSeconds(10)
    val sqsStream: Stream[IO, Message] = sqs.messageStream(client, rmr)

    val pullRequestStream: Stream[IO, PR] = sqsStream
      .map(m => decode[PullRequestEvent](m.getBody))
      .collect { case Right(pr) => pr }
      .map { event => (
        event.pull_request.base.flatMap(_.repo),
        event.pull_request.base.flatMap(_.user).map(_.login),
        event.number
      ) }
      .collect { case (Some(repo), Some(login), n) =>
        val Array(owner, repoName) = repo.full_name.split("/")
        PR(owner, repoName, login, n)
      }

    // filter pr creator who are in the org

    // check the google sheet
    val hasSignedClaStream: Stream[IO, (PR, Boolean)] = pullRequestStream
      .evalMap { pr =>
        gsheets
          .get(conf.gsheets.spreadsheetId, conf.gsheets.sheetName, conf.gsheets.column)
          .map((pr, _))
      }
      .map { case (pr, signers) => (pr, signers.contains(pr.creator))}

    // post a message saying yes / no
  }
}
