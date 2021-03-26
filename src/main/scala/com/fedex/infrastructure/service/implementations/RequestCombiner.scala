package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.implicits._
import cats.kernel.Semigroup
import com.fedex.data.constants.ResponseDictionary
import com.fedex.infrastructure.data.adts.XyzQueryParam
import io.lemonlabs.uri.Url
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Promise}


trait RequestCombiner {

  private def getValuesFromMap(keys: Seq[String], innerMap: Map[String, JsValue]): Map[String, JsValue] =
    keys.map(k => (k, innerMap(k))).toMap

  def jsonOk(str: String): HttpResponse = {
    HttpResponse()
      .withStatus(StatusCodes.OK)
      .withEntity(str)
  }

  def getResp(req: HttpRequest, responseMapOpt: Option[Map[String, JsValue]]): HttpResponse = {
    responseMapOpt match {
      case Some(responseMap) =>
        val keys = Url.parse(req.uri.toString())
          .query
          .params
          .flatMap(_._2.getOrElse("").split(","))

        val values = getValuesFromMap(keys, responseMap)
        jsonOk(values.toJson.toString())

      case _ => ResponseDictionary.fallbackResponse
    }
  }

  def extractEntity[U](combinedResponse: HttpResponse): Option[HttpEntity] = {
    combinedResponse match {
      case HttpResponse(StatusCodes.OK, _, _, _) => Some(combinedResponse.entity)
      case _ => None
    }
  }

  def combineRequests(listOfRequests: Seq[(HttpRequest, Promise[HttpResponse])])
                     (implicit queryCombiner: Semigroup[XyzQueryParam], mat: Materializer, ec: ExecutionContext): (HttpRequest, Promise[HttpResponse]) = {

    val responsePromise = Promise[HttpResponse]()

    responsePromise.future.foreach { combinedResponse: HttpResponse =>
      extractEntity(combinedResponse).map { combinedResponseEntity: HttpEntity =>
        Unmarshal(combinedResponseEntity).to[String]
          .map(_.parseJson.convertTo[Map[String, JsValue]])

      }.sequence
        .foreach { values =>
          listOfRequests.foreach { case (req, prom) =>
            prom.success(getResp(req, values))
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