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

  def findCollaborator(repo: Repository, user: User): F[Option[String]]
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

  def findCollaborator(repo: Repository, user: User): F[Option[String]] =
    gh.repos.listCollaborators(repo.owner.login, repo.name)
      .exec[F, HttpResponse[String]]()
      .flatMap(extractResult)
      .map(users => users.map(_.login).find(_ === user.login))
}

object GithubService {
  val noMessage = """Thanks for your pull request. Is this your first contribution to a Snowplow open source project? Before we can look at your pull request, you'll need to sign a Contributor License Agreement (CLA).
                    |
                    |:memo: Please visit https://github.com/snowplow/snowplow/wiki/CLA to learn more and sign.
                    |
                    |Once you've signed, please reply here (e.g. I signed it!) and we'll verify. Thanks.
                    |""".stripMargin

  def thanksMessage(login: String) = s"Confirmed! @$login has signed the Contributor License Agreement. Thanks so much."

  sealed abstract class ClaLabel(val value: String)
  final case object YesLabel extends ClaLabel("cla:yes")
  final case object NoLabel extends ClaLabel("cla:no")
}
