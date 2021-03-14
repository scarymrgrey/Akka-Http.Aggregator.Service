package com.fedex.routes

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.fedex.formats.JsonFormats
import com.fedex.services._
import sttp.client3.Response

import scala.concurrent.Future

class ShipmentRoutes(shipmentService: XyzService[Future, HttpResponse],aggService: AggXyzService[Future,HttpResponse])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers: Future[HttpResponse] =
    shipmentService.getShipments

  def getAgg: Future[HttpResponse] =
    aggService.getAggregatedOr(HttpResponse().withEntity("Error"))

  val routes: Route = {
    concat(
      pathPrefix("shipments") {
      pathEnd {
        get {
          onSuccess(getUsers) { allParts =>
            complete {
              allParts
            }
          }
        }

      }
    }
      ,
      pathPrefix("aggregations") {
        pathEnd {
          get {
            onSuccess(getAgg) { allParts =>
              complete {
                allParts
              }
            }
          }

        }
      }
    )
  }
}
