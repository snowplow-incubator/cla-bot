/*
 * Copyright (c) 2018-2018 Snowplow Analytics Ltd. All rights reserved.
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
import cats.effect.{ContextShift, IO}
import github4s.free.domain.{Comment, Label}
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.implicits._

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito

import config._
import GithubService.{NoLabel, YesLabel}
import model._

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

  override def findCollaborator(organization: model.Repository, user: model.User): IO[Option[String]] =
    if(user.login === "userCollaborator")
      IO.pure(Some("userCollaborator"))
    else
      IO.pure(None)
}

class WebhookRoutesSpec
    extends Specification with Mockito {
  implicit val cs: ContextShift[IO] =
    IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

  val sampleRepo = Repository("repo", User("owner"))

  private def prEventRequest(login: String) = {
    val prHeaders = Headers(Header("X-GitHub-Event", "pull_request"))
    val body      = Stream.emits(PullRequestEvent("opened", 1, sampleRepo, User(login)).asJson.noSpaces.getBytes)

    Request[IO](method = Method.POST, uri = Uri.uri("/webhook"),
                headers = prHeaders, body = body)
  }

  private def commentEventRequest(login: String, number: Int) = {
    val issueHeaders = Headers(Header("X-GitHub-Event", "issue_comment"))
    val body         = Stream.emits(IssueCommentEvent("created", Issue(number, User(login).some), sampleRepo, User(login)).asJson.noSpaces.getBytes)

    Request[IO](method = Method.POST, uri = Uri.uri("/webhook"),
                headers = issueHeaders, body = body)
  }

  val config = CLAConfig(
    IndividualCLAConfig(
      "spreadsheet-id",
      "sheet-name",
      "column"
    ),
    List("ignore")
  )

  "The service" should {
    "add a label and comment on new pull request if user has not signed CLA" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("userWithNoCla")).unsafeRunSync()

      there was one(gsheetsMock).findLogin("userWithNoCla")
      there was one(githubMock)
        .addLabel(Repository("repo", User("owner")), Issue(1), NoLabel)
      there was one(githubMock)
        .postComment(Repository("repo", User("owner")), Issue(1), GithubService.noMessage)
      /**gsheetsMock wasCalled onceOn findLogin(eqTo("userWithNoCla"))
      githubMock wasCalled onceOn addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1, None)),
        eqTo(NoLabel)
      )
      githubMock wasCalled onceOn postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1, None)),
        eqTo(GithubService.noMessage)
      )**/
    }

    "add a label but not comment if user has signed CLA" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("userWithCla")).unsafeRunSync()

      there was one(gsheetsMock).findLogin("userWithCla")
      there was one(githubMock)
        .addLabel(Repository("repo", User("owner")), Issue(1), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")

      /**gsheetsMock wasCalled onceOn findLogin(eqTo("userWithCla"))
      githubMock wasCalled onceOn addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo(YesLabel)
      )
      githubMock was never called on postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo("")
      )**/
    }

    "not do anything if user is a collaborator" in {

      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("userCollaborator")).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")

      /**gsheetsMock was never called on findLogin(eqTo("userCollaborator"))
      githubMock was never called on addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo(YesLabel)
      )
      githubMock was never called on postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo("")
      )**/
    }

    "not do anything if user is in the list of people to ignore" in {

      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("ignore")).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")

      /**gsheetsMock was never called on findLogin(eqTo("userCollaborator"))
      githubMock was never called on addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo(YesLabel)
      )
      githubMock was never called on postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo("")
      )**/
    }

    "not do anything if pinged and there is no 'cla:no' label" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(commentEventRequest("userWithCla", 0)).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")

      /**gsheetsMock was never called on findLogin(eqTo("userWithCla"))
      githubMock was never called on addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo(YesLabel)
      )
      githubMock was never called on postComment(
        eqTo(Repository("repoo", User("owner"))),
        eqTo(Issue(1)),
        eqTo("")
      )**/
    }

    "not post a comment if pinged and there is a 'cla:no' label but user has not yet signed cla" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(commentEventRequest("userWithoutCla", 1)).unsafeRunSync()

      there was one(gsheetsMock).findLogin("userWithoutCla")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")

      /**gsheetsMock wasCalled onceOn findLogin(eqTo("userWithoutCla"))
      githubMock was never called on addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo(YesLabel)
      )
      githubMock was never called on postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1)),
        eqTo("")
      )**/
    }

    "post a comment if pinged, there is 'cla:no' label and user has signed the cla" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(commentEventRequest("userWithCla", 1)).unsafeRunSync()

      there was one(gsheetsMock).findLogin("userWithCla")
      there was one(githubMock).addLabel(
        Repository("repo", User("owner")), Issue(1, Some(User("userWithCla"))), YesLabel)
      there was one(githubMock).postComment(
        Repository("repo", User("owner")),
        Issue(1, Some(User("userWithCla"))),
        GithubService.thanksMessage("userWithCla")
      )

      /**gsheetsMock wasCalled onceOn findLogin(eqTo("userWithCla"))
      githubMock wasCalled onceOn addLabel(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1, Some(User("userWithCla")))),
        eqTo(YesLabel)
      )
      githubMock wasCalled onceOn postComment(
        eqTo(Repository("repo", User("owner"))),
        eqTo(Issue(1, Some(User("userWithCla")))),
        eqTo(GithubService.thanksMessage("userWithCla"))
      )**/
    }
  }

}
