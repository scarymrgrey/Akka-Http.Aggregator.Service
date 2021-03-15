package com.fedex.infrastructure

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import cats.kernel.Semigroup
import com.fedex.formats.ResponseDictionary
import com.fedex.infrastructure.adts.XyzQuery
import com.fedex.services.XyzServiceBus

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class XyzHttpServiceBusConfigs(maxElements: Int,queueSize: Int,  finiteDuration: FiniteDuration)

trait XyzHttpServiceBusFactory {
  def newQueueFor(endpoint: String)(
    implicit confs: XyzHttpServiceBusConfigs,
     queryCombiner: Semigroup[XyzQuery]): XyzServiceBus[Future, HttpRequest, HttpResponse]
}

object XyzHttpServiceBusFactory {
  def dsl: XyzHttpServiceBusFactory = new XyzHttpServiceBusFactory {
    override def newQueueFor(endpoint: String)
                            (implicit confs: XyzHttpServiceBusConfigs, queryCombiner: Semigroup[XyzQuery]):
    XyzServiceBus[Future, HttpRequest, HttpResponse] = new XyzServiceBus[Future, HttpRequest, HttpResponse] {

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

      private def combineRequests(listOfRequests: Seq[(HttpRequest, Promise[HttpResponse])]): (HttpRequest, Promise[HttpResponse]) = {
        val responsePromise = Promise[HttpResponse]()
        responsePromise.future.onComplete(f => listOfRequests.foreach(p => p._2.complete(f)))
        val newQuery = listOfRequests
          .map(_._1.uri.toString())
          .map(XyzQuery)
          .reduce(queryCombiner.combine)

        val value: Uri = Uri.apply(newQuery.query)
        val newReq = listOfRequests.head._1.withUri(value)
        newReq -> responsePromise
      }

      private val poolClientFlow = Http().newHostConnectionPool[Promise[HttpResponse]]("localhost", 8888)

      private val queue =
        Source.queue[(HttpRequest, Promise[HttpResponse])](confs.queueSize, OverflowStrategy.backpressure)
          .async
          .groupedWithin(confs.maxElements, confs.finiteDuration)
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

