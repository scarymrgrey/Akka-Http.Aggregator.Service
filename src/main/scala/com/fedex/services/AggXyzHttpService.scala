package com.fedex.services

import cats._
import cats.syntax.all._
import com.fedex.infrastructure.core.configs.WithSettings
import com.fedex.infrastructure.service.implementations.XyzHttpService
import com.fedex.data.typeclasses.TimedOutSyntax._
import com.fedex.data.typeclasses.Taggable
import com.fedex.data.typeclasses.{HttpResponseTaggable, TimedOut}

import scala.concurrent.duration.DurationInt

object AggXyzHttpService {
  def dsl[F[_] : Applicative : TimedOut, Out: Taggable : Semigroup]
  (implicit xyzService: XyzHttpService[F, Out])
  : AggXyzService[F, Out] = new AggXyzService[F, Out] with WithSettings {

    import xyzService._
    import HttpResponseTaggable._

    override def getAggregatedOr(default: Out)(pricing: Option[String], track: Option[String], shipments: Option[String]): F[Out] =
      (
        getPricing(pricing).setTimeOut(aggConfs.timeOut seconds, default),
        getTrack(track).setTimeOut(aggConfs.timeOut seconds, default),
        getShipments(shipments).setTimeOut(aggConfs.timeOut seconds, default))
        .mapN((price, track, shipment) => {
          List("price" -> price, "track" -> track, "shipment" -> shipment)
            .map {
              case (tag, out) => out.applyTag(tag)
            }.reduce(_ |+| _)
        })
  }
}
