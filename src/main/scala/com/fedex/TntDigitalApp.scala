package com.fedex

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import cats.instances.future._
import com.fedex.infrastructure.service.implementations.{XyzHttpService, XyzHttpServiceBusFactory}
import com.fedex.routes.AggregationsRoute
import com.fedex.services.AggXyzHttpService

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object TntDigitalApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {

    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context: ActorContext[Nothing] =>
      import com.fedex.data.typeclasses.HttpResponseTaggable._
      import com.fedex.data.typeclasses.TimedOutInstances._
      import com.fedex.data.composers.HttpResponseSemigroupInstances._

      implicit val ac: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContextExecutor = context.system.executionContext

      implicit val xyz: XyzHttpService[Future, HttpResponse] = XyzHttpService.dsl(XyzHttpServiceBusFactory.dsl)
      val shipmentR: AggregationsRoute = new AggregationsRoute(AggXyzHttpService.dsl[Future, HttpResponse])(context.system)

      startHttpServer {
        shipmentR.routes
      }(context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "TNTAkkaServer")
  }
}
