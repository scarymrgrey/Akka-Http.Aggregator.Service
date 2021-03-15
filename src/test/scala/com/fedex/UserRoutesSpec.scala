package com.fedex

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class UserRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem


  def createOkWith(jsonString: String): Future[HttpResponse] =
    Future.successful(HttpResponse()
      .withStatus(StatusCodes.OK)
      .withEntity(ContentTypes.`application/json`, jsonString))

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

  val userRegistry = new AggXyzService[Future, HttpResponse] {
    override def getAggregatedOr(default: HttpResponse)(pricing: Option[String], track: Option[String], shipments: Option[String]): Future[HttpResponse] =
      (pricing, track, shipments) match {
        case (None, None, None) =>
          createOkWith(ResponseConstants.emptyBody)

        case (None, None, Some(_)) =>
          createOkWith(ResponseConstants.hasShipments)

        case _ => Future.failed(???)
      }

  }
  lazy val routes = new AggregationsRoute(userRegistry).routes

  "AggregationsRoute" should {
    "return nulls if no inputs (GET /aggregations)" in {
      val request = HttpRequest(uri = "/aggregations")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(ResponseConstants.emptyBody)
      }
    }

    "return no users if no present (GET /aggregations?shipments=109347263,123456891)" in {
      val request = HttpRequest(uri = "/aggregations?shipments=109347263,123456891")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(ResponseConstants.hasShipments)
      }
    }
  }
}

