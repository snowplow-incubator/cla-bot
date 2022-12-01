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

import cats.data.NonEmptyList
import cats.implicits._

import cats.effect.IO
import cats.effect.unsafe.IORuntime

import fs2.Stream

import io.circe.syntax._

import org.http4s._
import org.http4s.implicits._

import github4s.domain.{Comment, Label}

import org.typelevel.ci.CIString

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito

import clabot.config._
import clabot.GithubService.{NoLabel, YesLabel}
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
      IO.pure(List(Label(NoLabel.value, "foo", None, None, None)))

  override def addLabel(
    repo: Repository,
    issue: Issue,
    label: GithubService.ClaLabel
  ): IO[List[Label]] = IO.pure(List.empty)

  override def removeNoLabel(repo: Repository, issue: Issue): IO[List[Label]] = IO.pure(List.empty)

  override def postComment(repo: Repository, issue: Issue, text: String): IO[Comment] =
    IO.pure(Comment(0L, "foo", "foo", "foo", "foo", "foo", None))

  override def findCollaborator(organization: Repository, user: User): IO[Option[String]] =
    if(user.login === "userCollaborator")
      IO.pure(Some("userCollaborator"))
    else
      IO.pure(None)
}

class WebhookRoutesSpec extends Specification with Mockito {

  implicit val runtime = IORuntime.global

  val sampleRepo = Repository("repo", User("owner"))

  private def prEventRequest(login: String) = {
    val prHeaders = Headers(Header.Raw(CIString("X-GitHub-Event"), "pull_request"))
    val body = Stream.emits(
      PullRequestEvent("opened", 1, sampleRepo, User(login)).asJson.noSpaces.getBytes)

    Request[IO](method = Method.POST, uri = uri"/webhook", headers = prHeaders, body = body)
  }

  private def commentEventRequest(login: String, number: Int) = {
    val issueHeaders = Headers(Header.Raw(CIString("X-GitHub-Event"), "issue_comment"))
    val body = Stream.emits(
      IssueCommentEvent("created", Issue(number, User(login).some), sampleRepo, User(login))
        .asJson.noSpaces.getBytes
    )

    Request[IO](
      method = Method.POST, uri = uri"/webhook", headers = issueHeaders, body = body)
  }

  val config = CLAConfig(
    GoogleSheet("spreadsheet-id", "sheet-name", NonEmptyList.one("column")),
    GoogleSheet("spreadsheet-id", "sheet-name", NonEmptyList.one("column")),
    GoogleSheet("spreadsheet-id", "sheet-name", NonEmptyList.one("column")),
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
    }

    "not do anything if user is a collaborator" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("userCollaborator")).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")
    }

    "not do anything if user is in the list of people to ignore" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(prEventRequest("ignore")).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")
    }

    "not do anything if pinged and there is no 'cla:no' label" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(commentEventRequest("userWithCla", 0)).unsafeRunSync()

      there was no(gsheetsMock).findLogin("")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")
    }

    "not post a comment if pinged and there is a no label but user has not yet signed the cla" in {
      val gsheetsMock = spy(new GsheetsServiceTestImpl)
      val githubMock  = spy(new GithubServiceTestImpl)
      val endpoints = new WebhookRoutes[IO](gsheetsMock, githubMock, config).routes.orNotFound

      endpoints(commentEventRequest("userWithoutCla", 1)).unsafeRunSync()

      there was one(gsheetsMock).findLogin("userWithoutCla")
      there was no(githubMock).addLabel(Repository("", User("")), Issue(0), YesLabel)
      there was no(githubMock).postComment(Repository("", User("")), Issue(0), "")
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
    }
  }

}
