package clabot

object config {
  case class GithubConfig(token: String)
  case class AwsConfig(sqsQueueUrl: String)
  case class GSheetsConfig(token: String)
  case class ClaBotConfig(
    github: GithubConfig,
    aws: AwsConfig,
    gsheets: GSheetsConfig
  )
}
