package clabot

import cats.implicits._
import cats.effect.IO

import github4s.Github
import github4s.Github._
import github4s.GithubResponses.GHResponse
import github4s.cats.effect.jvm.Implicits._
import github4s.free.domain.{Comment, Label}

import scalaj.http.HttpResponse

import model._

class GithubService(token: String) {

  import GithubService._

  val gh = Github(Some(token))

  private def extractResult[A](response: GHResponse[A]): IO[A] =
    IO.fromEither(response.map(_.result))


  def listLabels(repo: Repository, issue: Issue): IO[List[Label]] =
    gh.issues.listLabels(repo.owner.login, repo.name, issue.number)
      .exec[IO, HttpResponse[String]]()
      .flatMap(extractResult)


  def addLabel(repo: Repository, issue: Issue, label: ClaLabel): IO[List[Label]] =
    gh.issues.addLabels(repo.owner.login, repo.name, issue.number, List(label.value))
      .exec[IO, HttpResponse[String]]()
      .flatMap(extractResult)


  def removeNoLabel(repo: Repository, issue: Issue): IO[List[Label]] =
    gh.issues.removeLabel(repo.owner.login, repo.name, issue.number, NoLabel.value)
      .exec[IO, HttpResponse[String]]()
      .flatMap(extractResult)


  def postComment(repo: Repository, issue: Issue, text: String): IO[Comment] =
    gh.issues.createComment(repo.owner.login, repo.name, issue.number, text)
      .exec[IO, HttpResponse[String]]()
      .flatMap(extractResult)


  def findMember(organization: Organization, user: User): IO[Option[String]] =
    gh.organizations.listMembers(organization.login)
      .exec[IO, HttpResponse[String]]()
      .flatMap(extractResult)
      .map(users => users.map(_.login).find(_ === user.login))

}

object GithubService {

  val noMessage = "Please sign the CLA: https://github.com/snowplow/snowplow/wiki/CLA.\n" +
    "Once you signed it, comment on this PR to trigger the bot."

  val thanksMessage = "Thank you for signing the CLA!"


  sealed abstract class ClaLabel(val value: String)
  final case object YesLabel extends ClaLabel("cla:yes")
  final case object NoLabel extends ClaLabel("cla:no")
}
