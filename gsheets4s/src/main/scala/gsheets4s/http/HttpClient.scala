package gsheets4s.http

import cats.Monad
import cats.effect.Ref
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._

import org.http4s.{ Method, Uri }

import io.circe.{Encoder, Decoder}
import io.lemonlabs.uri.Url

import gsheets4s.model.{Credentials, GsheetsError}

class HttpClient[F[_]: Monad](creds: Ref[F, Credentials], requester: HttpRequester[F])(implicit urls: GSheets4sDefaultUrls) {
  def get[O](path: Url, params: List[(String, String)] = List.empty)
            (implicit d: Decoder[O]): F[Either[GsheetsError, O]] =
    req(token => requester
        .request[Either[GsheetsError, O]](urlBuilder(token, path, params), Method.GET))

  def put[I, O](path: Url, body: I, params: List[(String, String)] = List.empty)
               (implicit e: Encoder[I], d: Decoder[O]): F[Either[GsheetsError, O]] =
    req(token => requester.requestWithBody[I, Either[GsheetsError, O]](
      urlBuilder(token, path, params), body, Method.PUT))

  def post[I, O](
    path: Url,
    body: I,
    params: List[(String, String)] = List.empty)(
    implicit e: Encoder[I], d: Decoder[O]): F[Either[GsheetsError, O]] =
      req(token => requester.requestWithBody[I, Either[GsheetsError, O]](
        urlBuilder(token, path, params), body, Method.POST))

  private def req[O](req: String => F[Either[GsheetsError, O]]): F[Either[GsheetsError, O]] = for {
    c <- creds.get
    first <- req(c.accessToken)
    retried <- first match {
      case Left(GsheetsError(401, _, _)) => reqWithNewToken(req, c)
      case o => Monad[F].pure(o)
    }
  } yield retried

  private def reqWithNewToken[O](
    req: String => F[Either[GsheetsError, O]], c: Credentials): F[Either[GsheetsError, O]] = for {
      newToken <- refreshToken(c)(Decoder.decodeString.prepare(_.downField("access_token")))
      _ <- creds.set(c.copy(accessToken = newToken))
      r <- req(newToken)
    } yield r

  private def refreshToken(c: Credentials)(implicit d: Decoder[String]): F[String] = {
    val url = urls.refreshTokenUrl.setQueryParams(Map(
      ("refresh_token" -> List(c.refreshToken)),
        ("client_id" -> List(c.clientId)),
        ("client_secret" -> List(c.clientSecret)),
        ("grant_type" -> List("refresh_token"))))
    requester.request(url, Method.POST)
  }

  private def urlBuilder(accessToken: String, path: Url, params: List[(String, String)]): Uri =
    (urls.baseUrl / path.show).setQueryParams(Map(("access_token" -> List(accessToken))) ++ params.map(kv => kv._1 -> List(kv._2)).toMap)
}

