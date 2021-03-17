package com.fedex.data.composers

import akka.NotUsed
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.scaladsl.{Merge, Source}
import akka.util.ByteString
import cats.Semigroup

object HttpResponseSemigroupInstances {
  implicit object HttpResponseSemigroup extends Semigroup[HttpResponse] {
    override def combine(l: HttpResponse, r: HttpResponse): HttpResponse = {
      val src: Source[ByteString, NotUsed] = Source.combine(l.entity.dataBytes, r.entity.dataBytes)(Merge(_))
      HttpResponse().withEntity(HttpEntity(ContentTypes.`application/json`, src))
    }
  }
}
