/*
 * Copyright (c) 2018-2020 Snowplow Analytics Ltd. All rights reserved.
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

import cats.NonEmptyParallel
import cats.implicits._

import cats.effect._

import com.typesafe.config.ConfigFactory

import io.circe.config.syntax._
import io.circe.generic.auto._

import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder

import gsheets4s.model.Credentials

import clabot.config._

object Main extends ResourceApp.Simple {
  def run = runServer[IO]

  def runServer[F[_]: Async: NonEmptyParallel] =
    for {
      config        <- Resource.eval(getConfig[F])
      httpClient    <- BlazeClientBuilder[F].resource
      sheetService  <- Resource.eval(getSheetsService[F](httpClient, config.gsheets, config.cla.individualCLA, config.cla.corporateCLA))
      githubService  = new GithubServiceImpl[F](httpClient, config.github.token)
      webhookRoutes  = new WebhookRoutes[F](sheetService, githubService, config.cla)
      _             <- BlazeServerBuilder[F].bindHttp(config.port, config.host).withHttpApp(webhookRoutes.routes.orNotFound).resource
    } yield ()

  def getConfig[F[_]: Sync]: F[CLABotConfig] =
    Sync[F].delay(ConfigFactory.load().as[CLABotConfig]).flatMap(Sync[F].fromEither)

  def getSheetsService[F[_]: Concurrent](
    httpClient: Client[F],
    config: GSheetsConfig,
    individualCLA: GoogleSheet,
    corporateCLA: GoogleSheet
  ): F[GSheetsService[F]] =
    Ref.of[F, Credentials](config.toCredentials)
      .map(credsRef => new GSheetsServiceImpl[F](httpClient, credsRef, individualCLA, corporateCLA))

}
