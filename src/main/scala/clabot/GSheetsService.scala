package clabot

import cats.data.EitherT
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.syntax.either._

import eu.timepit.refined.api.RefType

import gsheets4s._
import gsheets4s.model._

class GSheetsService(credentials: Credentials, spreadsheetId: String, sheetName: String, column: String) {
  private implicit val interpreter = hammock.jvm.Interpreter[IO]


  def findLogin(login: String): IO[Option[String]] =
    getAll.map(logins => logins.find(_ === login))


  private val getAll: IO[List[String]] = {
    val program = for {
      credsRef          <- EitherT.right(Ref.of[IO, Credentials](credentials))
      col               <- EitherT.fromEither[IO](colPosition)
      spreadsheetValues =  GSheets4s[IO](credsRef).spreadsheetsValues
      range             =  SheetNameRangeNotation(sheetName, Range(col, col))
      valueRange        <- EitherT(spreadsheetValues.get(spreadsheetId, range)).leftMap(_.toString)
    } yield valueRange.values.flatten

    program.value.map(either => either.leftMap(str => new RuntimeException(str))).flatMap(IO.fromEither)
  }


  private val colPosition: Either[String, ColPosition] =
    RefType.applyRef[Col](column).map(ColPosition)

}
