package clabot

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.either._
import eu.timepit.refined.auto._
import eu.timepit.refined.api.RefType
import fs2.async
import gsheets4s.interpreters.RestSpreadsheetsValues
import gsheets4s.model._

class Gsheets(credentials: Credentials) {
  private val credsRef = async.refOf[IO, Credentials](credentials)
  private implicit val interpreter = hammock.jvm.Interpreter[IO]

  def get(spreadsheetId: String, sheetName: String, column: String): IO[Either[String, List[String]]] = (for {
    creds <- EitherT.right(credsRef)
    name <- EitherT.fromEither[IO](RefType.applyRef[SheetName](sheetName).leftMap(_.toString))
    col <- EitherT.fromEither[IO](RefType.applyRef[Col](column).map(ColPosition.apply).leftMap(_.toString))
    spreadsheetValues = RestSpreadsheetsValues(creds)
    valueRange <- EitherT(spreadsheetValues.get(
      spreadsheetId,
      SheetNameRangeNotation(name, Range(col, col))
    )).leftMap(_.toString)
  } yield valueRange.values.flatten).value

}
