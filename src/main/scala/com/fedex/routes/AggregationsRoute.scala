package com.fedex.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.fedex.data.constants.ResponseDictionary
import com.fedex.infrastructure.exceptions.GlobalExceptionHandler.globalExceptionHandler
import com.fedex.services._

import scala.concurrent.Future

class AggregationsRoute(aggService: AggXyzService[Future, HttpResponse])(implicit val system: ActorSystem[_]) {

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("tnt-aggregator-app.routes.ask-timeout"))

  private def getAgg: (Option[String], Option[String], Option[String]) => Future[HttpResponse] = {
    aggService.getAggregatedOr(ResponseDictionary.fallbackResponse)
  }

  val routes: Route = {
    handleExceptions(globalExceptionHandler) {
      pathPrefix("aggregation") {
        pathEnd {
          get {
            parameters("pricing".optional, "track".optional, "shipments".optional) { (pricing, track, shipments) =>
              onSuccess(getAgg(pricing, track, shipments)) { allParts =>
                complete(allParts)
              }
            }
          }
        }
      }
    }
  }
}
