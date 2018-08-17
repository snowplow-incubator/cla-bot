package clabot

import cats.data.EitherT
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.either._

import eu.timepit.refined.api.RefType

import gsheets4s._
import gsheets4s.model._

class GSheetsService[F[_]: Sync](credentials: Ref[F, Credentials], spreadsheetId: String, sheetName: String, column: String) {
  private implicit val interpreter = hammock.jvm.Interpreter[F]

  def findLogin(login: String): F[Option[String]] =
    getAll.map(logins => logins.find(_ === login))

  private val colPosition: Either[String, ColPosition] =
    RefType.applyRef[Col](column).map(ColPosition)

  private val getAll: F[List[String]] = {
    val program = for {
      col               <- EitherT.fromEither[F](colPosition)
      spreadsheetValues =  GSheets4s[F](credentials).spreadsheetsValues
      range             =  SheetNameRangeNotation(sheetName, Range(col, col))
      valueRange        <- EitherT(spreadsheetValues.get(spreadsheetId, range)).leftMap(_.toString)
    } yield valueRange.values.flatten

    program.value.map(either => either.leftMap(str => new RuntimeException(str))).flatMap(Sync[F].fromEither)
  }



}
