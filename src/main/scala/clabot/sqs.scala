package clabot

import scala.collection.JavaConverters._

import cats.effect.IO
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model._
import fs2._

/** Utility object providing fs2 abstractions over the SQS API. */
object sqs {
  /**
   * fs2.Stream of messages coming from SQS.
   * @param client of AWS SQS
   * @param request ReceiveMessageRequest being constantly sent to SQS
   * @return an fs2.Stream of SQS Message
   */
  def messageStream(
    client: AmazonSQSAsync, request: ReceiveMessageRequest
  ): Stream[IO, Message] =
    Stream
      .repeatEval(receive(client, request))
      .flatMap(result => Stream.emits(result.getMessages.asScala))

  /**
   * fs2.Sink deleting messages from SQS.
   * @param client of AWS SQS
   * @param request lambda producing a DeleteMessageRequest from a Message
   * @return an fs2.Sink deleting messages
   */
  def deleteSink(
    client: AmazonSQSAsync, request: Message => DeleteMessageRequest
  ): Sink[IO, Message] = _
    .map(m => request(m))
    .evalMap(delete(client, _))
    .map(_ => ())

  /**
   * IO pulling messages from AWS SQS.
   * @param client of AWS SQS
   * @param request ReceiveMessageRequest sent to SQS
   * @return a ReceiveMessageResult wrapped in an IO
   */
  def receive(
    client: AmazonSQSAsync, request: ReceiveMessageRequest
  ): IO[ReceiveMessageResult] =
    IO.async[ReceiveMessageResult] { cb =>
      client.receiveMessageAsync(request, handler[ReceiveMessageRequest, ReceiveMessageResult](cb))
    }

  /**
   * IO deleting messages from AWS SQS.
   * @param client of AWS SQS
   * @param request DeleteMessageRequest sent to SQS
   * @return a DeleteMessageResult wrapped in an IO
   */
  def delete(
    client: AmazonSQSAsync, request: DeleteMessageRequest
  ): IO[DeleteMessageResult] =
    IO.async[DeleteMessageResult] { cb =>
      client.deleteMessageAsync(request, handler[DeleteMessageRequest, DeleteMessageResult](cb))
    }

  private def handler[E <: AmazonWebServiceRequest, A](
    cb: Either[Throwable, A] => Unit
  ): AsyncHandler[E, A] =
    new AsyncHandler[E, A] {
      override def onError(e: Exception): Unit = cb(Left(e))
      override def onSuccess(request: E, result: A): Unit = cb(Right(result))
    }
}
