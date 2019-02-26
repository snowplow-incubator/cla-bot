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
import clabot.config._

object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    ServerStream.stream[IO].compile.lastOrError
}

object ServerStream {

  def getConfig[F[_]: Sync]: F[CLABotConfig] =
    Sync[F].fromEither(ConfigFactory.load().as[CLABotConfig])

  def getSheetsService[F[_]: Sync](
    config: GSheetsConfig,
    claConfig: IndividualCLAConfig
  ): F[GSheetsService[F]] =
    Ref.of[F, Credentials](config.toCredentials)
      .map(credentialsRef =>
        new GSheetsServiceImpl[F](credentialsRef,
          claConfig.spreadsheetId,
          claConfig.sheetName,
          claConfig.column)
      )

  def getGithubService[F[_]: Sync](token: String): GithubService[F] =
    new GithubServiceImpl[F](token)

  def stream[F[_]: ConcurrentEffect: NonEmptyParallel1: Timer]: Stream[F, ExitCode] =
    for {
      config         <- Stream.eval(getConfig[F])
      sheetService   <- Stream.eval(getSheetsService[F](config.gsheets, config.cla.individualCLA))
      githubService  =  getGithubService[F](config.github.token)
      webhookRoutes  =  new WebhookRoutes[F](sheetService, githubService, config.cla)
      exitCode       <- BlazeServerBuilder[F]
        .bindHttp(config.port, config.host)
        .withHttpApp(webhookRoutes.routes.orNotFound)
        .serve
    } yield exitCode

}
