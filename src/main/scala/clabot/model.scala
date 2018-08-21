package clabot

import io.circe.generic.JsonCodec

object model {

  /**
   * Event emitted by the github webhook for pull request.
   * @param action which triggered the event, can be opened, reopened, synchronized, closed
   * @param number of the pull request
   * @param repository data about the repository
   * @param sender data about the creator of the PR
   */
  @JsonCodec final case class PullRequestEvent(
    action: String,
    number: Int,
    repository: Repository,
    sender: User,
  )


  /**
    * Event emitted by the github webhook for issues (the PR is also treated as an issue). Used here for listening to
    * PR comments.
    * @param action what triggered the event, we are only interested in `created`
    * @param issue issue object containing the issue number
    * @param repository data about the repository
    * @param sender data about the commenter
    */
  @JsonCodec final case class IssueCommentEvent(
    action: String,
    issue: Issue,
    repository: Repository,
    sender: User
  )


  @JsonCodec final case class Issue(number: Int, user: Option[User] = None)
  @JsonCodec final case class Repository(name: String, owner: User)
  @JsonCodec final case class User(login: String) // shared between `sender` and `owner` fields
}
