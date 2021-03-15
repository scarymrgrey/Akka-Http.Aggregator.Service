package com.fedex.formats

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}

object ResponseDictionary {
  def fallbackResponse: HttpResponse = HttpResponse()
    .withStatus(StatusCodes.OK)
    .withEntity("null")
}
