package com.fedex.services

import cats._
import cats.syntax.all._
import com.fedex.infrastructure.service.implementations.XyzHttpService
import com.fedex.typeclasses.TimedOutSyntax._
import com.fedex.typeclasses.Taggable
import com.fedex.typeclasses.{HttpResponseTaggable, TimedOut}

import scala.concurrent.duration.DurationInt

object AggXyzHttpService {
  def dsl[F[_] : Applicative : TimedOut, Out: Taggable : Semigroup]
  (implicit xyzService: XyzHttpService[F, Out])
  : AggXyzService[F, Out] = new AggXyzService[F, Out] {

    import xyzService._
    import HttpResponseTaggable._

    override def getAggregatedOr(default: Out)(pricing: Option[String], track: Option[String], shipments: Option[String]): F[Out] =
      (
        getPricing(pricing).setTimeOut(40.seconds, default),
        getTrack(track).setTimeOut(40.seconds, default),
        getShipments(shipments).setTimeOut(40.seconds, default))
        .mapN((price, track, shipment) => {
          List("price" -> price, "track" -> track, "shipment" -> shipment)
            .map {
              case (tag, out) => out.applyTag(tag)
            }.reduce(_ |+| _)
        })
  }
}
