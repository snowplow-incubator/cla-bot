package clabot

import cats.data.EitherT
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

class GSheetsServiceImpl[F[_]: Sync](credentials: Ref[F, Credentials], spreadsheetId: String, sheetName: String, column: String)
  extends GSheetsService[F] {

  import GSheetsService._

  def findLogin(login: String): F[Option[String]] =
    getAll.map(logins => logins.find(_ === login))

  private val colPosition: Either[GSheetsException, ColPosition] =
    RefType.applyRef[Col](column).map(ColPosition).leftMap(msg => GSheetsException(msg))

  private val getAll: F[List[String]] = {
    val program = for {
      col               <- EitherT.fromEither[F](colPosition)
      spreadsheetValues =  GSheets4s[F](credentials).spreadsheetsValues
      range             =  SheetNameRangeNotation(sheetName, Range(col, col))
      valueRange        <- EitherT(spreadsheetValues.get(spreadsheetId, range)).leftMap(e => GSheetsException(e.message))
    } yield valueRange.values.flatten

    program.value.flatMap(Sync[F].fromEither)
  }
}

object GSheetsService {

  case class GSheetsException(msg: String) extends RuntimeException(msg)

}
