/*
 * Copyright (c) 2018-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package clabot

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

object model {

  /**
   * Event emitted by the github webhook for pull request.
   * @param action which triggered the event, can be opened, reopened, synchronized, closed
   * @param number of the pull request
   * @param repository data about the repository
   * @param sender data about the creator of the PR
   */
  final case class PullRequestEvent(
    action: String,
    number: Int,
    repository: Repository,
    sender: User,
  )

  implicit val ioCircePullRequestEventDecoder: Decoder[PullRequestEvent] =
    deriveDecoder[PullRequestEvent]
  implicit val ioCircePullRequestEventEncoder: Encoder[PullRequestEvent] =
    deriveEncoder[PullRequestEvent]


  /**
    * Event emitted by the github webhook for issues (the PR is also treated as an issue).
    * Used here for listening to PR comments.
    * @param action what triggered the event, we are only interested in `created`
    * @param issue issue object containing the issue number
    * @param repository data about the repository
    * @param sender data about the commenter
    */
  final case class IssueCommentEvent(
    action: String,
    issue: Issue,
    repository: Repository,
    sender: User
  )

  implicit val ioCirceIssueCommentEventDecoder: Decoder[IssueCommentEvent] =
    deriveDecoder[IssueCommentEvent]
  implicit val ioCirceIssueCommentEventEncoder: Encoder[IssueCommentEvent] =
    deriveEncoder[IssueCommentEvent]


  final case class Issue(number: Int, user: Option[User] = None)

  implicit val ioCirceIssueDecoder: Decoder[Issue] =
    deriveDecoder[Issue]
  implicit val ioCirceIssueEncoder: Encoder[Issue] =
    deriveEncoder[Issue]

  final case class Repository(name: String, owner: User)

  implicit val ioCirceRepositoryDecoder: Decoder[Repository] =
    deriveDecoder[Repository]
  implicit val ioCirceRepositoryEncoder: Encoder[Repository] =
    deriveEncoder[Repository]

  final case class User(login: String) // shared between `sender` and `owner` fields

  implicit val ioCirceUserDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val ioCirceUserEncoder: Encoder[User] =
    deriveEncoder[User]
}
