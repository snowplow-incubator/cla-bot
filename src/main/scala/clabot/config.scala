package clabot

import gsheets4s.model.Credentials

object config {
  case class GithubConfig(token: String)
  case class AwsConfig(sqsQueueUrl: String)
  case class GSheetsConfig(
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
  case class ClaBotConfig(
    github: GithubConfig,
    aws: AwsConfig,
    gsheets: GSheetsConfig
  )
}
