package com.fedex.data.formats
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.json._
import DefaultJsonProtocol._
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

}