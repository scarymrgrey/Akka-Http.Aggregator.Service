package com.fedex.services

trait XyzServiceBus[F[_], In, Out] {
  def queueRequest(request: Option[String]): F[Out]
}
