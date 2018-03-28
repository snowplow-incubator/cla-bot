package clabot

import java.util.concurrent.{Future, FutureTask}

import cats.effect.IO
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model._
import com.amazonaws.handlers.AsyncHandler
import fs2._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{FlatSpec, Inspectors, Matchers}
import org.scalatest.mockito.MockitoSugar.mock

class SQSSpec extends FlatSpec with Matchers with Inspectors {

  "sqs" should "provide a message stream" in {
    val req = new ReceiveMessageRequest("url")
    val msg = new Message()
    val res = new ReceiveMessageResult().withMessages(msg)
    val client = mock[AmazonSQSAsync]
    when(
      client.receiveMessageAsync(
        any[ReceiveMessageRequest],
        any[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]]
      )
    ).thenAnswer(answer(req, res))

    val result = sqs.messageStream(client, req).take(3).compile.toVector.unsafeRunSync()
    forAll(result) { r => r shouldBe msg }
  }

  it should "provide a delete sink" in {
    val req = new DeleteMessageRequest("url", "receiptHandle")
    val res = new DeleteMessageResult()
    val client = mock[AmazonSQSAsync]
    when(
      client.deleteMessageAsync(
        any[DeleteMessageRequest],
        any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]]
      )
    ).thenAnswer(answer(req, res))

    val result = Stream(1, 2, 3)
      .covary[IO]
      .map(_ => new Message())
      .to(sqs.deleteSink(client, _ => req))
      .compile.toVector.unsafeRunSync()
    forAll(result) { r => r shouldBe(()) }
  }

  it should "provide a receive task" in {
    val req = new ReceiveMessageRequest("url")
    val res = new ReceiveMessageResult().withMessages(new Message())
    val client = mock[AmazonSQSAsync]
    when(
      client.receiveMessageAsync(
        any[ReceiveMessageRequest],
        any[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]]
      )
    ).thenAnswer(answer(req, res))

    val result = sqs.receive(client, req).unsafeRunSync()
    result shouldBe res
  }

  it should "provide a receive task which can fail" in {
    val req = new ReceiveMessageRequest("url")
    val client = mock[AmazonSQSAsync]
    when(
      client.receiveMessageAsync(
        any[ReceiveMessageRequest],
        any[AsyncHandler[ReceiveMessageRequest, ReceiveMessageResult]]
      )
    ).thenAnswer(failingAnswer())

    val result = sqs.receive(client, req).attempt.unsafeRunSync().toOption
    result shouldBe None
  }

  it should "provide a delete task" in {
    val req = new DeleteMessageRequest("url", "receiptHandle")
    val res = new DeleteMessageResult()
    val client = mock[AmazonSQSAsync]
    when(
      client.deleteMessageAsync(
        any[DeleteMessageRequest],
        any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]]
      )
    ).thenAnswer(answer(req, res))

    val result = sqs.delete(client, req).unsafeRunSync()
    result shouldBe res
  }

  it should "provide a delete task which can fail" in {
    val req = new DeleteMessageRequest("url", "receiptHandle")
    val client = mock[AmazonSQSAsync]
    when(
      client.deleteMessageAsync(
        any[DeleteMessageRequest],
        any[AsyncHandler[DeleteMessageRequest, DeleteMessageResult]]
      )
    ).thenAnswer(failingAnswer())

    val result = sqs.delete(client, req).attempt.unsafeRunSync().toOption
    result shouldBe None
  }

  def failingAnswer[REQ <: AmazonWebServiceRequest, RES](): Answer[Future[REQ]] =
    new Answer[Future[REQ]] {
      def answer(invocation: InvocationOnMock): Future[REQ] = {
        invocation.getArguments()(1).asInstanceOf[AsyncHandler[REQ, RES]].onError(new Exception)
        mock[FutureTask[REQ]]
      }
    }

  def answer[REQ <: AmazonWebServiceRequest, RES](req: REQ, res: RES): Answer[Future[REQ]] =
    new Answer[Future[REQ]] {
      def answer(invocation: InvocationOnMock): Future[REQ] = {
        invocation.getArguments()(1).asInstanceOf[AsyncHandler[REQ, RES]].onSuccess(req, res)
        mock[FutureTask[REQ]]
      }
    }
}
