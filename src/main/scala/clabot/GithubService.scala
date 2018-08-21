package clabot

import cats.implicits._
import cats.effect.Sync

import github4s.Github
import github4s.Github._
import github4s.GithubResponses.GHResponse
import github4s.cats.effect.jvm.Implicits._
import github4s.free.domain.{Comment, Label}

import scalaj.http.HttpResponse

import model._

trait GithubService[F[_]] {

  import GithubService._

  def listLabels(repo: Repository, issue: Issue): F[List[Label]]

  def addLabel(repo: Repository, issue: Issue, label: ClaLabel): F[List[Label]]

  def removeNoLabel(repo: Repository, issue: Issue): F[List[Label]]

  def postComment(repo: Repository, issue: Issue, text: String): F[Comment]

  def findMember(organization: Organization, user: User): F[Option[String]]

}

class GithubServiceImpl[F[_]: Sync](token: String) extends GithubService[F] {

  import GithubService._

  val gh = Github(Some(token))

  private def extractResult[A](response: GHResponse[A]): F[A] =
    Sync[F].fromEither(response.map(_.result))


  def listLabels(repo: Repository, issue: Issue): F[List[Label]] =
    gh.issues.listLabels(repo.owner.login, repo.name, issue.number)
      .exec[F, HttpResponse[String]]()
      .flatMap(extractResult)


  def addLabel(repo: Repository, issue: Issue, label: ClaLabel): F[List[Label]] =
    gh.issues.addLabels(repo.owner.login, repo.name, issue.number, List(label.value))
      .exec[F, HttpResponse[String]]()
      .flatMap(extractResult)


  def removeNoLabel(repo: Repository, issue: Issue): F[List[Label]] =
    gh.issues.removeLabel(repo.owner.login, repo.name, issue.number, NoLabel.value)
      .exec[F, HttpResponse[String]]()
      .flatMap(extractResult)


  def postComment(repo: Repository, issue: Issue, text: String): F[Comment] =
    gh.issues.createComment(repo.owner.login, repo.name, issue.number, text)
      .exec[F, HttpResponse[String]]()
      .flatMap(extractResult)


  def findMember(organization: Organization, user: User): F[Option[String]] =
    gh.organizations.listMembers(organization.login)
      .exec[F, HttpResponse[String]]()
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
