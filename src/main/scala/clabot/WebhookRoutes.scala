/*
 * Copyright (c) 2018-2019 Snowplow Analytics Ltd. All rights reserved.
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

import cats.implicits._
import cats.data.OptionT
import cats.effect._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString

import config.CLAConfig
import GithubService._
import model.{Issue, IssueCommentEvent, PullRequestEvent}
import NonEmptyParallel1.nonEmptyParallelFromNonEmptyParallel1

class WebhookRoutes[F[_]: Sync: NonEmptyParallel1](
  sheetsService: GSheetsService[F],
  githubService: GithubService[F],
  claConfig: CLAConfig
) extends Http4sDsl[F] {
  def routes = HttpRoutes.of[F] {
    case req @ POST -> Root / "webhook" =>
      val eventType = req.headers
        .get(CaseInsensitiveString("X-GitHub-Event"))
        .map(_.value)

      eventType match {
        case Some("pull_request")  =>
          req.as[PullRequestEvent]
            .flatMap(handlePullRequest(_, claConfig.peopleToIgnore))
            .attempt.flatMap {
              case Left(error) => InternalServerError(error.getMessage)
              case Right(_) => Ok()
            }

        case Some("issue_comment") =>
          req.as[IssueCommentEvent]
            .flatMap(handleIssueComment)
            .attempt.flatMap {
              case Left(error) => InternalServerError(error.getMessage)
              case Right(_) => Ok()
            }

        case Some("ping")          => Ok("pong")
        case Some(otherEventType)  => BadRequest(s"Unknown event type $otherEventType")
        case None                  => BadRequest("Malformed webhook format")
      }
  }

  def handlePullRequest(prEvent: PullRequestEvent, peopleToIgnore: List[String]): F[Unit] =
    if (prEvent.action === "opened") {
      handleNewPullRequest(prEvent, peopleToIgnore)
    } else {
      Sync[F].unit
    }

  def handleNewPullRequest(prEvent: PullRequestEvent, peopleToIgnore: List[String]): F[Unit] =
    if (peopleToIgnore.contains(prEvent.sender)) {
      Sync[F].unit
    } else {
      OptionT(githubService.findCollaborator(prEvent.repository, prEvent.sender))
        .map(_ => ())
        .orElse(handleNotCollaborator(prEvent))
        .getOrElseF(handleNoCla(prEvent))
    }

  def handleNotCollaborator(prEvent: PullRequestEvent): OptionT[F, Unit] =
    OptionT(sheetsService.findLogin(prEvent.sender.login))
      .semiflatMap(_ => githubService.addLabel(prEvent.repository, Issue(prEvent.number), YesLabel)
      .map(_ => ()))

  def handleNoCla(prEvent: PullRequestEvent): F[Unit] = {
    val addLabel = githubService.addLabel(prEvent.repository, Issue(prEvent.number), NoLabel)
    val comment  = githubService.postComment(prEvent.repository, Issue(prEvent.number), noMessage)

    (addLabel, comment).parMapN((_, _) => ())
  }

  def handleIssueComment(commentEvent: IssueCommentEvent): F[Unit] =
    if (commentEvent.action == "created") {
      handleNewComment(commentEvent)
    } else {
      Sync[F].unit
    }


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

        (addLabel, removeLabel, comment).parMapN((_, _, _) => ())
      }
      .getOrElseF(Sync[F].unit)
  }

}
