package gsheets4s.interpreters

import io.circe.generic.auto._
import io.lemonlabs.uri.typesafe.dsl._

import gsheets4s.algebras.SpreadsheetsValues
import gsheets4s.http._
import gsheets4s.model._

class RestSpreadsheetsValues[F[_]](client: HttpClient[F]) extends SpreadsheetsValues[F] {
  def get(spreadsheetID: String, range: A1Notation): F[Either[GsheetsError, ValueRange]] =
    client.get(spreadsheetID / "values" / range)

  def update(
    spreadsheetID: String,
    range: A1Notation,
    updates: ValueRange,
    valueInputOption: ValueInputOption
  ): F[Either[GsheetsError, UpdateValuesResponse]] =
    client.put(spreadsheetID / "values" / range, updates,
      List(("valueInputOption", valueInputOption.value)))
}
