package clabot

import cats.implicits._
import cats.effect.IO

import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode
import org.http4s.server.blaze._

import clabot.Config.ClaBotConfig

import concurrent.ExecutionContext.Implicits.global

object Server extends StreamApp[IO] {

  val getConfig: IO[ClaBotConfig] = {
    val configEither = pureconfig.loadConfig[ClaBotConfig]
      .leftMap(failures => new RuntimeException(failures.toList.map(_.description).mkString("\n  * ")))

    IO.fromEither(configEither)
  }


  def getSheetsService(config: ClaBotConfig): GSheetsService =
    new GSheetsService(config.gsheets.toCredentials,
                       config.gsheets.spreadsheetId,
                       config.gsheets.sheetName,
                       config.gsheets.column)


  def getGithubService(config: ClaBotConfig): GithubService =
    new GithubService(config.github.token)


  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    for {
      config         <- Stream.eval(getConfig)
      sheetService   =  getSheetsService(config)
      githubService  =  getGithubService(config)
      webhookService =  new WebhookService(sheetService, githubService)

      exitCode       <- BlazeBuilder[IO]
        .bindHttp(config.port)
        .mountService(webhookService.endpoints)
        .serve
    } yield exitCode

}
