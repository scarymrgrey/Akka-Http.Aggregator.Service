package com.fedex.formats

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

object ResponseDictionary {

  val respBodyJsonString = "null"

  def fallbackResponse: HttpResponse = HttpResponse()
    .withStatus(StatusCodes.OK)
    .withEntity(respBodyJsonString)
}
