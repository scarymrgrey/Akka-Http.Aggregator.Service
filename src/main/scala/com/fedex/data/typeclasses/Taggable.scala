package com.fedex.data.typeclasses

import akka.http.scaladsl.model.{HttpEntity, HttpResponse}
import akka.util.ByteString

trait Taggable[Out] {
  def applyTag(fa: Out)(tag: String): Out
}

object HttpResponseTaggable {
  implicit object HttpResponseMonoid extends Taggable[HttpResponse] {
    override def applyTag(fa: HttpResponse)(tag: String): HttpResponse = fa.mapEntity(e => {
      val value = e.dataBytes.map(bs => ByteString(s""" \"$tag\" : ${bs.utf8String}, \n"""))
      HttpEntity(e.contentType, value)
    })
  }

  implicit class AddableOps[Out](value: Out) {
    def applyTag(tag: String)(implicit completableInstance: Taggable[Out]): Out = {
      completableInstance.applyTag(value)(tag)
    }
  }

}
