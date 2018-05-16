package clabot

import cats.effect.IO
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import scalaj.http.{Http, HttpResponse}

class Github(token: String) {

  implicit val formats = DefaultFormats

  val GITHUB_URI = "https://api.github.com"

  def postMessage(owner: String, repo: String, issue: String, message: String): IO[HttpResponse[String]] = IO {
    Http(s"$GITHUB_URI/repos/$owner/$repo/issues/$issue/comments")
      .header("Authorization", s"token $token")
      .postData(write(PostMessageRequest(message)))
      .asString
  }

  def listLabels(owner: String, repo: String, issue: String): IO[List[String]] = IO {
    val response = Http(s"$GITHUB_URI/repos/$owner/$repo/issues/$issue/labels")
      .header("Authorization", s"token $token")
      .asString
    parse(response.body).values.asInstanceOf[List[Map[String, String]]].flatMap(_.get("name"))
  }

  def addLabel(owner: String, repo: String, issue: String, label: String): IO[HttpResponse[String]] = IO {
    Http(s"$GITHUB_URI/repos/$owner/$repo/issues/$issue/labels")
      .header("Authorization", s"token $token")
      .postData(write(List(label)))
      .asString
  }

  def deleteLabel(owner: String, repo: String, issue: String, label: String): IO[HttpResponse[String]] = IO {
    Http(s"$GITHUB_URI/repos/$owner/$repo/issues/$issue/labels/$label")
      .header("Authorization", s"token $token")
      .method("DELETE")
      .asString
  }

  def listMembers(org: String): IO[List[String]] = IO {
    val response = Http(s"$GITHUB_URI/orgs/$org/members")
      .header("Authorization", s"token $token")
      .asString
    parse(response.body).values.asInstanceOf[List[Map[String, String]]].flatMap(_.get("login"))
  }

}

case class PostMessageRequest(body: String)
