package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import cats.kernel.Semigroup
import com.fedex.infrastructure.data.adts.XyzQueryParam

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Success, Try}
import io.lemonlabs.uri.Url
import spray.json._
import DefaultJsonProtocol._
import akka.stream.Materializer
import cats.Functor

trait RequestCombiner {

  private def getValuesFromMap(keys: Seq[String], innerMap: Map[String, JsValue]): Map[String, JsValue] =
    keys.map(k => (k, innerMap(k))).toMap

  private def decomposeHttpResponse(combinedResponse: Try[HttpResponse], requests: Seq[(HttpRequest, Promise[HttpResponse])])
                                   (implicit mat: Materializer, ec: ExecutionContext): Try[Future[Seq[(Promise[HttpResponse], HttpResponse)]]] = {

    val requestsWithParams = requests.map { case (req, promise) =>
      val keys = Url.parse(req.uri.toString())
        .query
        .params
        .flatMap(_._2.getOrElse("").split(","))

      (promise, keys)
    }

    combinedResponse.map { value =>
      Unmarshal(value.entity).to[String].map { string =>
        val responseMap = string.parseJson.convertTo[Map[String, JsValue]]
        requestsWithParams.map { case (promise, keys) =>
          val values = getValuesFromMap(keys, responseMap)
          val response = HttpResponse()
            .withEntity(HttpEntity(ContentTypes.`application/json`, values.toJson.prettyPrint))
          (promise, response)
        }
      }
    }
  }

  def combineRequests(listOfRequests: Seq[(HttpRequest, Promise[HttpResponse])])
                     (implicit queryCombiner: Semigroup[XyzQueryParam], mat: Materializer, ec: ExecutionContext): (HttpRequest, Promise[HttpResponse]) = {

    case class RequestWithParameters(params: Seq[String], httpRequest: HttpRequest, promise: Promise[HttpResponse])

    val responsePromise = Promise[HttpResponse]()
    
    responsePromise.future.onComplete(combinedResponse =>
      Functor[Try].compose[Future].compose[Seq]
        .map(decomposeHttpResponse(combinedResponse, listOfRequests)){
        case (promise, response) => promise.complete(Success(response))
      }
    )

    val newQuery = listOfRequests
      .map(_._1.uri.toString())
      .map(XyzQueryParam)
      .reduce(queryCombiner.combine)

    val newReq = listOfRequests.head._1.withUri(Uri(newQuery.query))
    newReq -> responsePromise
  }
}