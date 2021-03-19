package com.fedex

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.fedex.constants.ResponseConstants
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzService
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class AggregationRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem


  def createOkWith(jsonString: String): Future[HttpResponse] =
    Future.successful(HttpResponse()
      .withStatus(StatusCodes.OK)
      .withEntity(ContentTypes.`application/json`, jsonString))


  private val userRegistry = new AggXyzService[Future, HttpResponse] {
    override def getAggregatedOr(default: HttpResponse)(pricing: Option[String], track: Option[String], shipments: Option[String]): Future[HttpResponse] =
      (pricing, track, shipments) match {
        case (None, None, None) =>
          createOkWith(ResponseConstants.emptyBody)

        case (None, None, Some(_)) =>
          createOkWith(ResponseConstants.hasShipments)

        case (Some("return_exception"), None, Some(_)) =>
          Future.failed(new Exception("return_exception"))

        case _ => Future.failed(???)
      }

  }

  lazy val routes = new AggregationsRoute(userRegistry).routes

  "AggregationsRoute" should {
    "return nulls if no inputs (GET /aggregation)" in {
      val request = HttpRequest(uri = "/aggregation")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(ResponseConstants.emptyBody)
      }
    }

    "return shipments and nulls (GET /aggregation?shipments=109347263,123456891)" in {
      val request = HttpRequest(uri = "/aggregation?shipments=109347263,123456891")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(ResponseConstants.hasShipments)
      }
    }

    "return 500 with message when failed (GET /aggregation?pricing=return_exception)" in {
      val request = HttpRequest(uri = "/aggregation?pricing=return_exception")
      request ~> routes ~> check {
        status should ===(StatusCodes.InternalServerError)
        entityAs[String] should ===("Administrator notified")
      }
    }
  }
}
