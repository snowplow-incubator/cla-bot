package gsheets4s.http

import org.http4s.Uri
import org.http4s.implicits._

case class GSheets4sDefaultUrls(baseUrl: Uri, refreshTokenUrl: Uri)

object GSheets4sDefaultUrls {
  implicit val defaultUrls: GSheets4sDefaultUrls = GSheets4sDefaultUrls(
    uri"https://sheets.googleapis.com/v4/spreadsheets",
    uri"https://www.googleapis.com/oauth2/v4/token"
  )
}
