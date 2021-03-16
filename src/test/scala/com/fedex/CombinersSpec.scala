package com.fedex

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fedex.constants.ResponseConstants
import com.fedex.infrastructure.data.adts.XyzQueryParam
import com.fedex.infrastructure.service.implementations.RequestCombiner
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzService
import com.fedex.typeclasses.combiners.XyzQuerySemigroup
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class CombinersSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  "XyzQuerySemigroup" should {

    "correctly combine two values" in {
      val v1 = XyzQueryParam("/shipments?q=val1")
      val v2 = XyzQueryParam("/shipments?q=val2")
      val v3 = XyzQuerySemigroup.combine(v1, v2)
      v3.query should ===("/shipments?q=val1,val2")
    }

    "drop duplicates" in {
      val v1 = XyzQueryParam("/shipments?q=val1,val3")
      val v2 = XyzQueryParam("/shipments?q=val1,val2")
      val v3 = XyzQuerySemigroup.combine(v1, v2)
      v3.query should ===("/shipments?q=val1,val3,val2")
    }
  }

  "RequestCombiner" should {
    "correctly combine two requests" in {
      val rq = new RequestCombiner {}
      implicit val qc = XyzQuerySemigroup


      val request1 = HttpRequest(uri = "/shipments?q=val1,val2,val3,val4,val5")
      val promise1 = Promise[HttpResponse]()

      val request2 = HttpRequest(uri = "/shipments?q=val4,val5,val6,val7,val8")
      val promise2 = Promise[HttpResponse]()

      val input = List((request1, promise1), (request2, promise2))

      val (combinedRequest, combinedPromise): (HttpRequest, Promise[HttpResponse]) = rq.combineRequests(input)

      combinedRequest.uri.toString() should ===("/shipments?q=val1,val2,val3,val4,val5,val6,val7,val8")

      val surrogateResponse = HttpResponse().withEntity(
        """{
          |"val1": 14.242090605778,
          |"val2": 20.503467806384,
          |"val3": 30.503467806384,
          |"val4": 40.503467806384,
          |"val5": 50.503467806384,
          |"val6": 60.503467806384,
          |"val7": 70.503467806384,
          |"val8": 80.503467806384,
          |}""".stripMargin)

      combinedPromise.complete(Success(surrogateResponse))

      val body1 = Unmarshal(promise1.future.futureValue.entity).to[String]
      val exp1 = "{\n\"val1\": 14.242090605778,\n\"val2\": 20.503467806384,\n\"val3\": 30.503467806384,\n\"val4\": 40.503467806384,\n\"val5\": 50.503467806384\n}"
      body1 should ===(exp1)

      val body2 = Unmarshal(promise2.future.futureValue.entity).to[String]
      val exp2 = "{\n\"val4\": 40.503467806384,\n\"val5\": 50.503467806384,\n\"val6\": 60.503467806384,\n\"val7\": 70.503467806384,\n\"val8\": 80.503467806384,\n}"
      body2 should ===(exp2)
    }
  }
}
