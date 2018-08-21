package clabot

import concurrent.ExecutionContext.Implicits.global
import cats._
import cats.implicits._
import cats.effect.IO
import clabot.GithubService.{NoLabel, YesLabel}
import io.circe.syntax._
import github4s.free.domain.{Comment, Label}
import fs2.Stream
import org.http4s._
import org.http4s.implicits._
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.mockito.MockitoSugar.spy
import org.scalatest._
import clabot.model._

class GsheetsServiceTestImpl extends GSheetsService[IO] {

  override def findLogin(login: String): IO[Option[String]] = login match {
    case "userWithCla" => IO.pure(Option("userWithCla"))
    case _             => IO.pure(None)
  }
}

class GithubServiceTestImpl extends GithubService[IO] {
  override def listLabels(repo: model.Repository, issue: model.Issue): IO[List[Label]] =
    if(issue.number === 0)
      IO.pure(List.empty)
    else
      IO.pure(List(Label(None, NoLabel.value, "foo", "foo", None)))

  override def addLabel(repo: model.Repository, issue: model.Issue, label: GithubService.ClaLabel): IO[List[Label]] =
    IO.pure(List.empty)

  override def removeNoLabel(repo: model.Repository, issue: model.Issue): IO[List[Label]] = IO.pure(List.empty)

  override def postComment(repo: model.Repository, issue: model.Issue, text: String): IO[Comment] =
    IO.pure(Comment(0, "foo", "foo", "foo", None, "foo", "foo"))

  override def findMember(organization: model.Organization, user: model.User): IO[Option[String]] =
    if(user.login === "userMember")
      IO.pure(Some("userMember"))
    else
      IO.pure(None)
}

class Tests extends FlatSpec with IdiomaticMockito with ArgumentMatchersSugar {

  val sampleRepo = Repository("repo", User("owner"))

  private def prEventRequest(login: String) = {
    val prHeaders = Headers(Header("X-GitHub-Event", "pull_request"))
    val body      = Stream.emits(PullRequestEvent("opened", 1, sampleRepo, User(login), None).asJson.noSpaces.getBytes)

    Request[IO](method = Method.POST, uri = Uri.uri("/webhook"),
                headers = prHeaders, body = body)
  }

  private def commentEventRequest(login: String, number: Int) = {
    val issueHeaders = Headers(Header("X-GitHub-Event", "issue_comment"))
    val body         = Stream.emits(IssueCommentEvent("created", Issue(number, User(login).some), sampleRepo, User(login)).asJson.noSpaces.getBytes)

    Request[IO](method = Method.POST, uri = Uri.uri("/webhook"),
                headers = issueHeaders, body = body)
  }

  "The service" should "add a label and comment on new pull request if user has not signed CLA" in {
    val gsheetsMock = spy(new GsheetsServiceTestImpl)
    val githubMock  = spy(new GithubServiceTestImpl)
    val endpoints = new WebhookService[IO](gsheetsMock, githubMock).endpoints.orNotFound

    endpoints(prEventRequest("userWithNoCla")).unsafeRunSync()

    githubMock wasCalled onceOn addLabel(*, *, eqTo(NoLabel))
    githubMock wasCalled onceOn postComment(*, *, *)
  }

  it should "add a label but not comment if user has signed CLA" in {
    val gsheetsMock = spy(new GsheetsServiceTestImpl)
    val githubMock  = spy(new GithubServiceTestImpl)
    val endpoints = new WebhookService[IO](gsheetsMock, githubMock).endpoints.orNotFound

    endpoints(prEventRequest("userWithCla")).unsafeRunSync()

    githubMock wasCalled onceOn addLabel(*, *, eqTo(YesLabel))
    githubMock was never called on postComment(*, *, *)
  }

  it should "not do anything if pinged and there is no 'cla:no' label" in {
    val gsheetsMock = spy(new GsheetsServiceTestImpl)
    val githubMock  = spy(new GithubServiceTestImpl)
    val endpoints = new WebhookService[IO](gsheetsMock, githubMock).endpoints.orNotFound

    endpoints(commentEventRequest("userWithCla", 0)).unsafeRunSync()

    gsheetsMock was never called on findLogin(*)
    githubMock was never called on addLabel(*, *, *)
    githubMock was never called on postComment(*, *, *)
  }

  it should "not post a comment if pinged and there is a 'cla:no' label but user has not yet signed cla" in {
    val gsheetsMock = spy(new GsheetsServiceTestImpl)
    val githubMock  = spy(new GithubServiceTestImpl)
    val endpoints = new WebhookService[IO](gsheetsMock, githubMock).endpoints.orNotFound

    endpoints(commentEventRequest("userWithoutCla", 1)).unsafeRunSync()

    gsheetsMock wasCalled onceOn findLogin(*)
    githubMock was never called on addLabel(*, *, *)
    githubMock was never called on postComment(*, *, *)
  }

  it should "post a comment if pinged, there is 'cla:no' label and user has signed the cla" in {
    val gsheetsMock = spy(new GsheetsServiceTestImpl)
    val githubMock  = spy(new GithubServiceTestImpl)
    val endpoints = new WebhookService[IO](gsheetsMock, githubMock).endpoints.orNotFound

    endpoints(commentEventRequest("userWithCla", 1)).unsafeRunSync()

    gsheetsMock wasCalled onceOn findLogin(*)
    githubMock wasCalled onceOn addLabel(*, *, eqTo(YesLabel))
    githubMock wasCalled onceOn postComment(*, *, *)
  }

}
