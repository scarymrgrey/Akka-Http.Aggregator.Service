package com.fedex.infrastructure

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.fedex.services.XyzServiceBus

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object XyzHttpService {
  implicit val dsl : XyzServiceBus[Future, HttpRequest, HttpResponse] = new XyzServiceBus[Future, HttpRequest, HttpResponse] {

    import akka.actor.ActorSystem
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model._
    import akka.stream.scaladsl._
    import akka.stream.{OverflowStrategy, QueueOfferResult}

    import scala.concurrent.{Future, Promise}
    import scala.util.{Failure, Success}

    implicit val system = ActorSystem()

    import system.dispatcher // to get an implicit ExecutionContext into scope

    val QueueSize = 10

    val poolClientFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]]("localhost",8888)
    val queue =
      Source.queue[(HttpRequest, Promise[HttpResponse])](QueueSize, OverflowStrategy.backpressure , 10)
        .throttle(10, 5.second)
        .via(poolClientFlow)
        .to(Sink.foreach({
          case ((Success(resp), p)) => p.success(resp)
          case ((Failure(e), p)) => p.failure(e)
        }))
        .run()

    override def queueRequest(request: HttpRequest): Future[HttpResponse] = {
      val responsePromise = Promise[HttpResponse]()
      queue.offer(request -> responsePromise).flatMap {
        case QueueOfferResult.Enqueued => responsePromise.future
        case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
      }
    }
  }
}
