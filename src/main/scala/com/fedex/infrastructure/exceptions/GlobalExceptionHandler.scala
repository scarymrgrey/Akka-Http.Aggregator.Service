package com.fedex.infrastructure.exceptions

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives.{complete, extractUri}
import akka.http.scaladsl.server.ExceptionHandler

object GlobalExceptionHandler {
  implicit def globalExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex =>
        extractUri { uri =>
          println(s"Some logging to ELK here: ${uri} Exception: $ex")
          complete(HttpResponse(InternalServerError, entity = "Administrator notified"))
        }
    }
}