package com.fedex.services

import akka.http.scaladsl.model._

import scala.concurrent.Future
import cats._
import com.fedex.typeclasses.Completable
import cats.syntax.all._

import scala.concurrent.duration.DurationInt
import com.fedex.typeclasses.FutureCompletableSyntax._

trait XyzService[F[_], Out] {
  def getShipments: F[Out]

  def getTrack: F[Out]

  def getPricing: F[Out]
}

trait AggXyzService[F[_], Out] {
  def getAggregatedOr(default: Out): F[Out]
}

trait XyzServiceBus[F[_], In, Out] {
  def queueRequest(request: In): F[Out]
}

object XyzService {

  implicit def dsl(implicit
                   xyzService: XyzServiceBus[Future, HttpRequest, HttpResponse]
                  ): XyzService[Future, HttpResponse] = new XyzService[Future, HttpResponse] {

    import xyzService._

    override def getShipments: Future[HttpResponse] =
      queueRequest(HttpRequest(uri = "/shipments?q=109347263,123456891"))

    override def getTrack: Future[HttpResponse] =
      queueRequest(HttpRequest(uri = "/shipments?q=109347263,123456891"))

    override def getPricing: Future[HttpResponse] =
      queueRequest(HttpRequest(uri = "/shipments?q=109347263,123456891"))
  }
}


object AggXyzHttpService {
  def dsl[F[_] : Monad : Completable, Out](implicit
                                                                xyzService: XyzService[F, Out]
                                                               ): AggXyzService[F, Out] = new AggXyzService[F, Out] {



    override def getAggregatedOr(default: Out) : F[Out] = (for {
      price <- xyzService.getPricing.orDefault(2.seconds, default)
      track <- xyzService.getTrack.orDefault(2.seconds, default)
      shipment <- xyzService.getShipments.orDefault(2.seconds, default)
    } yield shipment)
  }

}


//#user-registry-actor
