package com.fedex

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import com.fedex.routes.{ShipmentRoutes, UserRoutes}
import com.fedex.services.{AggXyzHttpService, UserRegistry, XyzService}

import scala.util.Failure
import scala.util.Success
import akka.http.scaladsl.server.Directives._
import com.fedex.infrastructure.XyzHttpService
import com.fedex.typeclasses.Completable

import scala.concurrent.Future
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
      import XyzHttpService._
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      context.watch(userRegistryActor)
      val userR = new UserRoutes(userRegistryActor)(context.system)

      import cats.instances.future._
      import scala.concurrent.ExecutionContext.Implicits.global
      implicit val xyz = XyzService.dsl
      import com.fedex.typeclasses.CompletableInstances._
      implicit val ac: ActorSystem[Nothing] = context.system

      val shipmentR = new ShipmentRoutes(XyzService.dsl, AggXyzHttpService.dsl[Future,HttpResponse])(context.system)

      startHttpServer(userR.routes ~ shipmentR.routes)(context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
