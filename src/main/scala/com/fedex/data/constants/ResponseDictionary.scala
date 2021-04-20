package com.fedex.data.constants

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

object ResponseDictionary {

  val responseBodyJsonString = "null"

  def fallbackResponse: HttpResponse = HttpResponse()
    .withStatus(StatusCodes.OK)
    .withEntity(responseBodyJsonString)
}
