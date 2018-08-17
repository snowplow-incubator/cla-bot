package clabot

import gsheets4s.model.Credentials

object Config {

  final case class GithubConfig(token: String)

  final case class GSheetsConfig(
    accessToken: String,
    refreshToken: String,
    clientId: String,
    clientSecret: String,
    spreadsheetId: String,
    sheetName: String,
    column: String
  ) {
    def toCredentials: Credentials = Credentials(accessToken, refreshToken, clientId, clientSecret)
  }

  final case class ClaBotConfig(
    port: Int,
    github: GithubConfig,
    gsheets: GSheetsConfig
  )
}
