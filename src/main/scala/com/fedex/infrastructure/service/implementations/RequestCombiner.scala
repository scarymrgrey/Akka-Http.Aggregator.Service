package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.kernel.Semigroup
import com.fedex.data.constants.ResponseDictionary
import com.fedex.infrastructure.data.adts.XyzQueryParam
import io.lemonlabs.uri.Url
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Success, Try}


trait RequestCombiner {

  private def getValuesFromMap(keys: Seq[String], innerMap: Map[String, JsValue]): Map[String, JsValue] =
    keys.map(k => (k, innerMap(k))).toMap

  def getResp(req: HttpRequest, combinedResponse: HttpResponse)(implicit mat: Materializer, ec: ExecutionContext): Future[Try[HttpResponse]] = {

    val keys = Url.parse(req.uri.toString())
      .query
      .params
      .flatMap(_._2.getOrElse("").split(","))

    Unmarshal(combinedResponse.entity).to[String].map { string =>
      val responseMap = string.parseJson.convertTo[Map[String, JsValue]]
      val values = getValuesFromMap(keys, responseMap)
      val response = HttpResponse()
        .withEntity(HttpEntity(ContentTypes.`application/json`, values.toJson.prettyPrint))
      Success(response)
    }
  }

  def combineRequests(listOfRequests: Seq[(HttpRequest, Promise[HttpResponse])])
                     (implicit queryCombiner: Semigroup[XyzQueryParam], mat: Materializer, ec: ExecutionContext): (HttpRequest, Promise[HttpResponse]) = {

    val responsePromise = Promise[HttpResponse]()

    responsePromise.future.onComplete { combinedResponse: Try[HttpResponse] =>
      listOfRequests.foreach { case (req, prom) =>
        getResp(req, combinedResponse.get).onComplete {
          case Success(x) => prom.complete(x)
          case _ => prom.complete(Success(ResponseDictionary.fallbackResponse))
        }
      }
    }

    val newQuery =
      listOfRequests
        .map(_._1.uri.toString())
        .map(XyzQueryParam)
        .reduce(queryCombiner.combine)

    val newReq = listOfRequests.head._1.withUri(Uri(newQuery.query))
    newReq -> responsePromise
  }
}