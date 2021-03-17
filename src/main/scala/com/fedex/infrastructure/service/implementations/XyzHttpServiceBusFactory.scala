package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import cats.kernel.Semigroup
import com.fedex.data.constants.ResponseDictionary
import com.fedex.infrastructure.core.configs.WithSettings
import com.fedex.infrastructure.data.adts.XyzQueryParam
import com.fedex.services.XyzServiceBus

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}



trait XyzHttpServiceBusFactory {
  def newQueueFor(endpoint: String)(implicit queryCombiner: Semigroup[XyzQueryParam]): XyzServiceBus[Future, HttpRequest, HttpResponse]
}

object XyzHttpServiceBusFactory {
  def dsl: XyzHttpServiceBusFactory = new XyzHttpServiceBusFactory with WithSettings{
    override def newQueueFor(endpoint: String)(implicit queryCombiner: Semigroup[XyzQueryParam]):

    XyzServiceBus[Future, HttpRequest, HttpResponse] = new XyzServiceBus[Future, HttpRequest, HttpResponse] with RequestCombiner {

      import akka.actor.ActorSystem
      import akka.http.scaladsl.Http
      import akka.http.scaladsl.model._
      import akka.stream.scaladsl._
      import akka.stream.{OverflowStrategy, QueueOfferResult}

      import scala.concurrent.{Future, Promise}
      import scala.util.{Failure, Success}

      private implicit val system: ActorSystem = ActorSystem()

      import system.dispatcher

      private def recoverResponse(httpResponse: HttpResponse): HttpResponse = httpResponse match {
        case HttpResponse(StatusCodes.OK, _, _, _) => httpResponse
        case _ => ResponseDictionary.fallbackResponse
      }

      private val poolClientFlow = Http().newHostConnectionPool[Promise[HttpResponse]](xyzConfs.backendHost, xyzConfs.backendPort)

      private val queue =
        Source.queue[(HttpRequest, Promise[HttpResponse])](xyzConfs.queueSize, OverflowStrategy.backpressure)
          .async
          .groupedWithin(xyzConfs.maxElements, xyzConfs.finiteDurationSeconds seconds)
          .map(combineRequests)
          .via(poolClientFlow)
          .to(Sink.foreach({
            case ((Success(resp), p)) => p.success(recoverResponse(resp))
            case ((Failure(_), p)) => p.success(ResponseDictionary.fallbackResponse)
          }))
          .run()

      private def queueRequest(request: HttpRequest): Future[HttpResponse] = {
        val responsePromise = Promise[HttpResponse]()
        val eventualResult: Future[QueueOfferResult] = queue.offer(request -> responsePromise)
        eventualResult.flatMap {
          case QueueOfferResult.Enqueued => responsePromise.future
          case QueueOfferResult.Dropped => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
          case QueueOfferResult.Failure(ex) => Future.failed(ex)
          case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
        }
      }

      override def queueRequest(params: Option[String]): Future[HttpResponse] =
        params match {
          case Some(query) => queueRequest(HttpRequest(uri = s"/$endpoint?q=$query"))
          case _ => Future.successful(ResponseDictionary.fallbackResponse)
        }
    }
  }
}