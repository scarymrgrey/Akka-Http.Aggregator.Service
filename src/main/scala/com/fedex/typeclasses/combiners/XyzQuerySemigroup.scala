package com.fedex.typeclasses.combiners

import cats._
import com.fedex.infrastructure.adts.XyzQuery

object XyzQuerySemigroup extends Semigroup[XyzQuery] {

  private def splitIntoKV(value: XyzQuery): (String, String) = {
    val res = value.query.split("/?q=")
    (res.headOption.getOrElse(""), res.tail.headOption.getOrElse(""))
  }

  override def combine(x: XyzQuery, y: XyzQuery): XyzQuery = {

    val (key, _) = splitIntoKV(x)
    val vector = List(x, y)
      .map(splitIntoKV)
      .map(_._2)
      .mkString(",")
      .split(",")
      .distinct // in order to drop duplicates in parameters for q1&q2
      .mkString(",")

     XyzQuery(s"${key}q=$vector")
  }
}
