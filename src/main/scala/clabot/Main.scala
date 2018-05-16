package clabot

import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.config.syntax._

import config._

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
    // filter pr creator who are in the org
    // check the google sheet
    // post a message saying yes / no
  }
}
