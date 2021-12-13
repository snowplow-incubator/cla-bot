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

import cats.syntax.all._
import cats.effect._
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import config.GoogleSheet

import java.io.FileInputStream
import scala.jdk.CollectionConverters.CollectionHasAsScala

trait GSheetsService[F[_]] {
  def findLogin(login: String): F[Option[String]]
}

class GSheetsServiceImpl[F[_] : Sync](
                                       client: Ref[F, Sheets],
                                       individualCLA: GoogleSheet,
                                       corporateCLA: GoogleSheet
                                     ) extends GSheetsService[F] {

  def findLogin(login: String): F[Option[String]] = for {
    a <- getAll(individualCLA)
    b <- getAll(corporateCLA)
  } yield (a ++ b).find(_ === login)


  private def getAll(googleSheet: GoogleSheet): F[List[String]] = {
    client.get.map(client =>
      googleSheet.range.flatMap(range =>
        client.spreadsheets.values.get(googleSheet.spreadsheetId, range).execute
          .getValues
          .asScala.toList.flatMap(_.asScala.toList)
          .map(_.toString)
      )
    )
  }
}

object GSheetsService {
  final case class GSheetsException(msg: String) extends RuntimeException(msg)

  private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport
  private val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance

  def apply[F[_] : Sync](individualCLA: GoogleSheet,
                         corporateCLA: GoogleSheet,
                         credPath: String): F[GSheetsService[F]] = {

    val v = ServiceAccountCredentials.fromStream(new FileInputStream(credPath))
      .createScoped("https://www.googleapis.com/auth/spreadsheets")
    val service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(v))
      .setApplicationName("cla-bot").build

    Ref[F].of(service).map(sheetClient =>
      new GSheetsServiceImpl(sheetClient, individualCLA, corporateCLA))
  }
}
