/*
 * Copyright (c) 2015-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package clabot

import concurrent.ExecutionContext.Implicits.global

import cats.effect.concurrent.Ref
import cats.temp.par._
import cats.implicits._
import cats.effect.{ConcurrentEffect, IO, Sync}

import fs2.{Stream, StreamApp}
import fs2.StreamApp.ExitCode

import org.http4s.server.blaze._

import gsheets4s.model.Credentials

import clabot.Config.ClaBotConfig

object Server extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] =
    ServerStream.stream[IO]
}

object ServerStream {

  def getConfig[F[_]: Sync]: F[ClaBotConfig] = {
    val configEither = pureconfig.loadConfig[ClaBotConfig]
      .leftMap(failures => new RuntimeException(failures.toList.map(_.description).mkString("\n  * ")))

    Sync[F].fromEither(configEither)
  }

  def getSheetsService[F[_]: Sync](config: ClaBotConfig): F[GSheetsService[F]] =
    Ref.of[F, Credentials](config.gsheets.toCredentials)
      .map(credentialsRef =>
        new GSheetsServiceImpl[F](credentialsRef,
          config.gsheets.spreadsheetId,
          config.gsheets.sheetName,
          config.gsheets.column)
      )

  def getGithubService[F[_]: Sync](config: ClaBotConfig): GithubService[F] =
    new GithubServiceImpl[F](config.github.token)

  def stream[F[_]: ConcurrentEffect : Par]: Stream[F, ExitCode] =
    for {
      config         <- Stream.eval(getConfig[F])
      sheetService   <- Stream.eval(getSheetsService[F](config))
      githubService  =  getGithubService[F](config)
      webhookService =  new WebhookService[F](sheetService, githubService)

      exitCode       <- BlazeBuilder[F]
        .bindHttp(config.port)
        .mountService(webhookService.endpoints)
        .serve
    } yield exitCode

}
