package clabot

import concurrent.ExecutionContext.Implicits.global

import cats.data.OptionT
import cats.implicits._
import cats.effect._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString

import clabot.model.{Issue, IssueCommentEvent, PullRequestEvent}
import clabot.GithubService._


class WebhookService(sheetsService: GSheetsService, githubService: GithubService) {

  import org.http4s.circe.CirceEntityDecoder._

  val endpoints = HttpRoutes.of[IO] {
    case req @ POST -> Root / "webhook" =>
      val eventType = req.headers
        .get(CaseInsensitiveString("X-GitHub-Event"))
        .map(_.value)

      eventType match {
        case Some("pull_request")  => req.as[PullRequestEvent]
                                        .flatMap(handlePullRequest)
                                        .handleErrorWith(error => InternalServerError(error.getMessage))
                                        .flatMap(_ => Ok())

        case Some("issue_comment") => req.as[IssueCommentEvent]
                                        .flatMap(handleIssueComment)
                                        .handleErrorWith(error => InternalServerError(error.getMessage))
                                        .flatMap(_ => Ok())

        case Some("ping")          => Ok("pong")
        case Some(otherEventType)  => BadRequest(s"Unknown event type $otherEventType")
        case None                  => BadRequest("Malformed webhook format")
      }
  }


  def handlePullRequest(prEvent: PullRequestEvent): IO[Unit] =
    if (prEvent.action === "opened") {
      handleNewPullRequest(prEvent)
    } else {
      IO.unit
    }


  def handleNewPullRequest(prEvent: PullRequestEvent): IO[Unit] =
    OptionT(sheetsService.findLogin(prEvent.sender.login))
      .semiflatMap(_ => githubService.addLabel(prEvent.repository, Issue(prEvent.number), YesLabel).map(_ => ()))
      .getOrElseF(handleNoCla(prEvent))


  def handleNoCla(prEvent: PullRequestEvent): IO[Unit] =
    OptionT(IO.pure(prEvent.organization))
      .flatMapF(org => githubService.findMember(org, prEvent.sender))
      .semiflatMap(_ => IO.unit)
      .getOrElseF {
        val addLabel = githubService.addLabel(prEvent.repository, Issue(prEvent.number), NoLabel)
        val comment  = githubService.postComment(prEvent.repository, Issue(prEvent.number), noMessage)

        (addLabel, comment).parMapN((_, _) => ())
      }


  def handleIssueComment(commentEvent: IssueCommentEvent): IO[Unit] =
    if (commentEvent.action == "created") {
      handleNewComment(commentEvent)
    } else {
      IO.unit
    }


  def handleNewComment(commentEvent: IssueCommentEvent): IO[Unit] = {
    val foundLabel = githubService
      .listLabels(commentEvent.repository, commentEvent.issue)
      .map(_.find(_.name === NoLabel.value))

    OptionT(foundLabel)
      .flatMapF(_ => sheetsService.findLogin(commentEvent.sender.login))
      .semiflatMap { _ =>
        val addLabel = githubService.addLabel(commentEvent.repository, commentEvent.issue, YesLabel)
        val removeLabel = githubService.removeNoLabel(commentEvent.repository, commentEvent.issue)
        val comment = githubService.postComment(commentEvent.repository, commentEvent.issue, thanksMessage)

        (addLabel, removeLabel, comment).parMapN((_, _, _) => ())
      }
      .getOrElseF(IO.unit)
  }

}
