package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model.HttpResponse
import com.fedex.data.composers.XyzQuerySemigroup
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object XyzHttpService {

  implicit def dsl(implicit
                   xyzServiceFactory: XyzHttpServiceBusFactory
                  ): XyzHttpService[Future, HttpResponse] = new XyzHttpService[Future, HttpResponse] {

    import xyzServiceFactory._

    implicit val qc = XyzQuerySemigroup
    private val shipmentQueue = newQueueFor("shipments")
    private val trackQueue = newQueueFor("track")
    private val pricingQueue = newQueueFor("pricing")


    override def getShipments(query: Option[String]): Future[HttpResponse] =
      shipmentQueue.queueRequest(query)

    override def getTrack(query: Option[String]): Future[HttpResponse] =
      trackQueue.queueRequest(query)

    override def getPricing(query: Option[String]): Future[HttpResponse] =
      pricingQueue.queueRequest(query)
  }
}

trait XyzHttpService[F[_], Out] {

  def getShipments(query: Option[String]): F[Out]

  def getTrack(query: Option[String]): F[Out]

  def getPricing(query: Option[String]): F[Out]

}