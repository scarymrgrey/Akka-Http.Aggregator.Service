package com.fedex

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.fedex.data.composers.XyzQuerySemigroup
import com.fedex.infrastructure.data.adts.XyzQueryParam
import com.fedex.infrastructure.service.implementations.RequestCombiner
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.util.Success

class CombinersSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  def compareTwoJsons(j1: JsValue, j2: JsValue): Unit = {
    val set1 = j1.convertTo[Map[String, JsValue]].toSet
    val set2 = j2.convertTo[Map[String, JsValue]].toSet
    set1 should contain theSameElementsAs set2
  }


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

      val jsonBody =
        """{
          |"val1": 14.242090605778,
          |"val2": 20.503467806384,
          |"val3": 30.503467806384,
          |"val4": 40.503467806384,
          |"val5": 50.503467806384,
          |"val6": 60.503467806384,
          |"val7": 70.503467806384,
          |"val8": 80.503467806384
          |}""".stripMargin
      val surrogateResponse = HttpResponse().withEntity(jsonBody)
      combinedPromise.complete(Success(surrogateResponse))

      val body1 = promise1.future.flatMap(r => Unmarshal(r.entity).to[String].map(_.parseJson))
      val exp1 =
        """{
          |"val1": 14.242090605778,
          |"val2": 20.503467806384,
          |"val3": 30.503467806384,
          |"val4": 40.503467806384,
          |"val5": 50.503467806384
          |}""".stripMargin
          .parseJson
      whenReady(body1, Timeout(10 seconds))(compareTwoJsons(_, exp1))

      val body2 = promise2.future.flatMap(r => Unmarshal(r.entity).to[String].map(_.parseJson))
      val exp2 = {
        """{
          |"val4": 40.503467806384,
          |"val5": 50.503467806384,
          |"val6": 60.503467806384,
          |"val7": 70.503467806384,
          |"val8": 80.503467806384
          |}""".stripMargin
          .parseJson

      }
      whenReady(body2, Timeout(10 seconds))(compareTwoJsons(_, exp2))
    }
  }
}
