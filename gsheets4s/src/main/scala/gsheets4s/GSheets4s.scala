package gsheets4s

import cats.effect.{ Concurrent, Ref }

import org.http4s.client.Client

import gsheets4s.algebras._
import gsheets4s.http._
import gsheets4s.interpreters._
import gsheets4s.model._

case class GSheets4s[F[_]](spreadsheetsValues: SpreadsheetsValues[F])

object GSheets4s {
  def apply[F[_]: Concurrent](client: Client[F], creds: Ref[F, Credentials]): GSheets4s[F] = {
    val requester = new Http4sRequester[F](client)
    val httpClient = new HttpClient[F](creds, requester)
    val spreadsheetsValues = new RestSpreadsheetsValues(httpClient)
    GSheets4s(spreadsheetsValues)
  }
}
