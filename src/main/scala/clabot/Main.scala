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
    val config = ConfigFactory.load()

    config.as[ClaBotConfig] match {
      case Left(e) =>
        System.err.println(s"configuration error: $e")
        System.exit(1)
      case Right(c) =>
        main(c)
    }
  }

  def main(config: ClaBotConfig): Unit = {
    // get pr event stream
    val rmr = new ReceiveMessageRequest(config.aws.sqsQueueUrl)
      .withMaxNumberOfMessages(1)
      .withWaitTimeSeconds(10)
    val sqsStream: Stream[IO, Message] = sqs.messageStream(client, rmr)

    val pullRequestStream: Stream[IO, PR] = sqsStream
      .map(m => decode[PullRequestEvent](m.getBody))
      .collect { case Right(pr) => pr }
      .map { event => (
        event.pull_request.base.flatMap(_.repo),
        event.number
      ) }
      .collect { case (Some(repo), n) =>
        val Array(owner, repoName) = repo.full_name.split("/")
        PR(owner, repoName, n)
      }

    // filter pr creator who are in the org
    // check the google sheet
    // post a message saying yes / no
  }
}
