package com.fedex.services

import akka.http.scaladsl.model._
import com.fedex.infrastructure.XyzHttpServiceBusFactory
import com.fedex.typeclasses.combiners.XyzQuerySemigroup

import scala.concurrent.Future



trait AggXyzService[F[_], Out] {
  def getAggregatedOr(default: Out)(pricing: Option[String], track: Option[String], shipments: Option[String]): F[Out]
}

trait XyzServiceBus[F[_], In, Out] {
  def queueRequest(request: Option[String]): F[Out]
}





