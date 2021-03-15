package com.fedex.constants

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
object ResponseConstants {
  val emptyBody =
    """ "price" : null,
      | "shipment" : null,
      | "track" : null, """.stripMargin

  val hasShipments =
    """ "price" : null,
      | "shipment" : {"109347263":["pallet","box","envelope"],"123456891":["pallet"]},
      | "track" : null, """.stripMargin
}



