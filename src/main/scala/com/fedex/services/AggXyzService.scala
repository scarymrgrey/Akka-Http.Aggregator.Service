package com.fedex.services

trait AggXyzService[F[_], Out] {
  def getAggregatedOr(default: Out)(pricing: Option[String], track: Option[String], shipments: Option[String]): F[Out]
}







