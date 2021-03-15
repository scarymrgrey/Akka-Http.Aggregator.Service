package com.fedex

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.fedex.constants.ResponseConstants
import com.fedex.infrastructure.adts.XyzQuery
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzService
import com.fedex.typeclasses.combiners.XyzQuerySemigroup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class CombinersSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  "XyzQuerySemigroup" should {

    "correctly combine two values" in {
      val v1 = XyzQuery("/shipments?q=val1")
      val v2 = XyzQuery("/shipments?q=val2")
      val v3 = XyzQuerySemigroup.combine(v1,v2)
      v3.query should ===("/shipments?q=val1,val2")
    }

    "drop duplicates" in {
      val v1 = XyzQuery("/shipments?q=val1,val3")
      val v2 = XyzQuery("/shipments?q=val1,val2")
      val v3 = XyzQuerySemigroup.combine(v1,v2)
      v3.query should ===("/shipments?q=val1,val3,val2")
    }
  }
}