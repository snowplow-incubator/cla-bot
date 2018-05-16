package clabot

object model {
  /**
   * Event emitted by the github hooks for pull request.
   * @param action which triggered the event, can be opened, reopened, synchronized, closed
   * @param number of the pull request
   * @param pull_request data about the PR in a Github4s' PullRequest
   */
  case class PullRequestEvent(
    action: String,
    number: Int,
    repository: Repository,
    sender: Sender
  )

  case class IssueCommentEvent(
    action: String,
    issue: Issue,
    repository: Repository,
    sender: Sender
  )

  case class Issue(url: String, number: Int)

  case class Repository(full_name: String)
  case class Sender(login: String)

  /**
   * Internal representation of a PR with only the necessary components.
   * @param owner of the repository
   * @param repo name of the repository
   * @param creator name of the pull request creator
   * @param number of the pull pull request
   */
  case class PR(
    owner: String,
    repo: String,
    creator: String,
    number: Int
  )
}
