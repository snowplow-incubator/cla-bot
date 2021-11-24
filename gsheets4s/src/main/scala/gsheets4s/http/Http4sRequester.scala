package gsheets4s.http

import cats.effect.Concurrent

import io.circe.{Encoder, Decoder}

import org.http4s.{ Request, Method, Uri }
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client


class Http4sRequester[F[_]: Concurrent](client: Client[F]) extends HttpRequester[F] {

  def request[O](uri: Uri, method: Method)(implicit d: Decoder[O]): F[O] = {
    client.expect[O](Request[F](method = method, uri = uri))
  }

  def requestWithBody[I, O](uri: Uri, body: I, method: Method)(implicit e: Encoder[I], d: Decoder[O]): F[O] = {
    client.expect[O](Request[F](method, uri).withEntity(body))
  }
}

