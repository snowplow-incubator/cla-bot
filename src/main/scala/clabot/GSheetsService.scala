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

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.either._
import eu.timepit.refined.api.RefType
import gsheets4s._
import gsheets4s.model._

trait GSheetsService[F[_]] {
  def findLogin(login: String): F[Option[String]]
}

class GSheetsServiceImpl[F[_]: Sync](
  credentials: Ref[F, Credentials],
  spreadsheetId: String,
  sheetName: String,
  columns: NonEmptyList[String]
) extends GSheetsService[F] {

  import GSheetsService._

  def findLogin(login: String): F[Option[String]] =
    getAll.map(logins => logins.find(_ === login))

  private val colPosition: Either[GSheetsException, ColPosition] =
    RefType.applyRef[Col](columns.head).map(ColPosition).leftMap(msg => GSheetsException(msg))

  private val getAll: F[List[String]] = {
    val program = for {
      col               <- EitherT.fromEither[F](colPosition)
      spreadsheetValues =  GSheets4s[F](credentials).spreadsheetsValues
      range             =  SheetNameRangeNotation(sheetName, Range(col, col))
      valueRange        <- EitherT(spreadsheetValues.get(spreadsheetId, range))
        .leftMap(e => GSheetsException(e.message))
    } yield valueRange.values.flatten

    program.value.flatMap(Sync[F].fromEither)
  }
}

object GSheetsService {
  final case class GSheetsException(msg: String) extends RuntimeException(msg)
}
