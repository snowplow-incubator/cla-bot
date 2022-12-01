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

import cats.NonEmptyParallel
import cats.implicits._
import cats.data.OptionT

import cats.effect._

import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl

import clabot.config.CLAConfig
import clabot.GithubService._
import clabot.model.{Issue, IssueCommentEvent, PullRequestEvent}

import org.typelevel.ci._

class WebhookRoutes[F[_]: Concurrent: NonEmptyParallel](
  sheetsService: GSheetsService[F],
  githubService: GithubService[F],
  claConfig: CLAConfig
) extends Http4sDsl[F] {

  import WebhookRoutes._

  def routes = HttpRoutes.of[F] {
    case GET -> Root / "health" => Ok("OK")
    case req @ POST -> Root / "webhook" =>
      val eventType = req.headers
        .get(GitHubEvent)
        .map(_.head)

      eventType match {
        case Some(PullRequest)  =>
          req.as[PullRequestEvent]
            .flatMap(handlePullRequest(_, claConfig.peopleToIgnore))
            .attempt.flatMap {
              case Left(error) => InternalServerError(error.getMessage)
              case Right(_) => Ok()
            }

        case Some(IssueComment) =>
          req.as[IssueCommentEvent]
            .flatMap(handleIssueComment)
            .attempt.flatMap {
              case Left(error) => InternalServerError(error.getMessage)
              case Right(_) => Ok()
            }

        case Some(Ping)          => Ok("pong")
        case Some(otherEventType)  => BadRequest(s"Unknown event type $otherEventType")
        case None                  => BadRequest("Malformed webhook format")
      }
  }

  def handlePullRequest(prEvent: PullRequestEvent, peopleToIgnore: List[String]): F[Unit] =
    if (prEvent.action === "opened") handleNewPullRequest(prEvent, peopleToIgnore)
    else Concurrent[F].unit

  def handleNewPullRequest(prEvent: PullRequestEvent, peopleToIgnore: List[String]): F[Unit] =
    if (peopleToIgnore.contains(prEvent.sender.login)) Concurrent[F].unit
    else OptionT(githubService.findCollaborator(prEvent.repository, prEvent.sender))
      .void
      .orElse(handleNotCollaborator(prEvent))
      .getOrElseF(handleNoCla(prEvent))

  def handleNotCollaborator(prEvent: PullRequestEvent): OptionT[F, Unit] =
    OptionT(sheetsService.findLogin(prEvent.sender.login))
      .semiflatMap(_ => githubService.addLabel(prEvent.repository, Issue(prEvent.number), YesLabel).void)

  def handleNoCla(prEvent: PullRequestEvent): F[Unit] = {
    val addLabel = githubService.addLabel(prEvent.repository, Issue(prEvent.number), NoLabel)
    val comment  = githubService.postComment(prEvent.repository, Issue(prEvent.number), noMessage)

    (addLabel, comment).parTupled.void
  }

  def handleIssueComment(commentEvent: IssueCommentEvent): F[Unit] =
    if (commentEvent.action == "created") handleNewComment(commentEvent)
    else Concurrent[F].unit

  def handleNewComment(commentEvent: IssueCommentEvent): F[Unit] = {
    val foundLabel = githubService
      .listLabels(commentEvent.repository, commentEvent.issue)
      .map(_.find(_.name === NoLabel.value))

    OptionT.fromOption[F](commentEvent.issue.user.filter(user => user.login === commentEvent.sender.login))
      .productL(OptionT(foundLabel))
      .flatMapF(user => sheetsService.findLogin(user.login))
      .semiflatMap { login =>
        val addLabel = githubService
          .addLabel(commentEvent.repository, commentEvent.issue, YesLabel)
        val removeLabel = githubService.removeNoLabel(commentEvent.repository, commentEvent.issue)
        val comment = githubService
          .postComment(commentEvent.repository, commentEvent.issue, thanksMessage(login))

        (addLabel, removeLabel, comment).parTupled.void
      }
      .getOrElseF(Concurrent[F].unit)
  }

}

object WebhookRoutes {
  val GitHubEvent = CIString("X-GitHub-Event")

  val Ping = Header.Raw(GitHubEvent, "ping")
  val IssueComment = Header.Raw(GitHubEvent, "issue_comment")
  val PullRequest = Header.Raw(GitHubEvent, "pull_request")
}
