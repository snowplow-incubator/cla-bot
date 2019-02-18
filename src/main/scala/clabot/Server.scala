/*
 * Copyright (c) 2018-2018 Snowplow Analytics Ltd. All rights reserved.
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

import cats.effect.{ExitCode, IOApp, Timer}
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.effect.{ConcurrentEffect, IO, Sync}
import com.typesafe.config.ConfigFactory
import fs2.Stream
import gsheets4s.model.Credentials
import io.circe.config.syntax._
import io.circe.generic.auto._
import org.http4s.implicits._
import org.http4s.server.blaze._
import clabot.Config.ClaBotConfig

object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    ServerStream.stream[IO].compile.lastOrError
}

object ServerStream {

  def getConfig[F[_]: Sync]: F[ClaBotConfig] =
    Sync[F].fromEither(ConfigFactory.load().as[ClaBotConfig])

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

  def stream[F[_]: ConcurrentEffect: NonEmptyParallel1: Timer]: Stream[F, ExitCode] =
    for {
      config         <- Stream.eval(getConfig[F])
      sheetService   <- Stream.eval(getSheetsService[F](config))
      githubService  =  getGithubService[F](config)
      webhookRoutes  =  new WebhookRoutes[F](sheetService, githubService)
      exitCode       <- BlazeServerBuilder[F]
        .bindHttp(config.port)
        .withHttpApp(webhookRoutes.routes.orNotFound)
        .serve
    } yield exitCode

}
