package com.fedex.data.composers

import cats._
import com.fedex.infrastructure.data.adts.XyzQueryParam

object XyzQuerySemigroup extends Semigroup[XyzQueryParam] {

  private def splitIntoKV(value: XyzQueryParam): (String, String) = {
    val res = value.query.split("/?q=")
    (res.headOption.getOrElse(""), res.tail.headOption.getOrElse(""))
  }

  override def combine(x: XyzQueryParam, y: XyzQueryParam): XyzQueryParam = {

    val (key, _) = splitIntoKV(x)
    val vct = List(x, y)
      .map(splitIntoKV)
      .map(_._2)
      .mkString(",")
      .split(",")
      .distinct // in order to drop duplicates in parameters for q1&q2
      .mkString(",")

     XyzQueryParam(s"${key}q=$vct")
  }
}
