package com.fedex.data.constants

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

object ResponseDictionary {

  val respBodyJsonString = "null"

  def fallbackResponse: HttpResponse = HttpResponse()
    .withStatus(StatusCodes.OK)
    .withEntity(respBodyJsonString)
}
