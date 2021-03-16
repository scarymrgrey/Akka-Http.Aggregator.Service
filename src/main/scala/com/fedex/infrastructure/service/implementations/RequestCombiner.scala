package com.fedex.infrastructure.service.implementations

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import cats.kernel.Semigroup
import com.fedex.infrastructure.data.adts.XyzQueryParam

import scala.concurrent.{ExecutionContext, Promise}

trait RequestCombiner {
  def combineRequests(listOfRequests: Seq[(HttpRequest, Promise[HttpResponse])])(implicit queryCombiner: Semigroup[XyzQueryParam], ec: ExecutionContext)
  : (HttpRequest, Promise[HttpResponse]) = {
    val responsePromise = Promise[HttpResponse]()
    responsePromise.future.onComplete(f => listOfRequests.foreach(p => p._2.complete(f)))
    val newQuery = listOfRequests
      .map(_._1.uri.toString())
      .map(XyzQueryParam)
      .reduce(queryCombiner.combine)

    val newReq = listOfRequests.head._1.withUri(Uri(newQuery.query))
    newReq -> responsePromise
  }
}
