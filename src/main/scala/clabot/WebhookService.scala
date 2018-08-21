package clabot

import cats.temp.par._
import cats.data.OptionT
import cats.implicits._
import cats.effect._

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.util.CaseInsensitiveString

import clabot.model.{Issue, IssueCommentEvent, PullRequestEvent}
import clabot.GithubService._


class WebhookService[F[_]: Sync : Par](sheetsService: GSheetsService[F], githubService: GithubService[F])
  extends Http4sDsl[F] {

  import org.http4s.circe.CirceEntityDecoder._

  val endpoints = HttpRoutes.of[F] {
    case req @ POST -> Root / "webhook" =>
      val eventType = req.headers
        .get(CaseInsensitiveString("X-GitHub-Event"))
        .map(_.value)

      eventType match {
        case Some("pull_request")  => req.as[PullRequestEvent]
                                        .flatMap(handlePullRequest)
                                        .attempt.flatMap {
                                          case Left(error) => InternalServerError(error.getMessage)
                                          case Right(_) => Ok()
                                        }

        case Some("issue_comment") => req.as[IssueCommentEvent]
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


  def handlePullRequest(prEvent: PullRequestEvent): F[Unit] =
    if (prEvent.action === "opened") {
      handleNewPullRequest(prEvent)
    } else {
      Sync[F].unit
    }


  def handleNewPullRequest(prEvent: PullRequestEvent): F[Unit] =
    OptionT(sheetsService.findLogin(prEvent.sender.login))
      .semiflatMap(_ => githubService.addLabel(prEvent.repository, Issue(prEvent.number), YesLabel).map(_ => ()))
      .getOrElseF(handleNoCla(prEvent))


  def handleNoCla(prEvent: PullRequestEvent): F[Unit] =
    OptionT(Sync[F].pure(prEvent.organization))
      .flatMapF(org => githubService.findMember(org, prEvent.sender))
      .semiflatMap(_ => Sync[F].unit)
      .getOrElseF {
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

    OptionT(foundLabel)
      .productR(OptionT.fromOption[F](commentEvent.issue.user))
      .flatMapF(user => sheetsService.findLogin(user.login))
      .semiflatMap { _ =>
        val addLabel = githubService.addLabel(commentEvent.repository, commentEvent.issue, YesLabel)
        val removeLabel = githubService.removeNoLabel(commentEvent.repository, commentEvent.issue)
        val comment = githubService.postComment(commentEvent.repository, commentEvent.issue, thanksMessage)

        (addLabel, removeLabel, comment).parMapN((_, _, _) => ())
      }
      .getOrElseF(Sync[F].unit)
  }

}
