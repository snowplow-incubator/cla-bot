package gsheets4s.http

import org.http4s.{Method, Uri}

import io.circe.{Encoder, Decoder}

trait HttpRequester[F[_]] {
  def request[O](uri: Uri, method: Method)(implicit d: Decoder[O]): F[O]
  def requestWithBody[I, O](uri: Uri, body: I, method: Method)(implicit e: Encoder[I], d: Decoder[O]): F[O]
}

