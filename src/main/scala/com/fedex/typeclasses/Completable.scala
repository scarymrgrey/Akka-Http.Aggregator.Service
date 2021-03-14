package com.fedex.typeclasses


import akka.actor.typed.ActorSystem
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

trait Completable[F[_]] {
  def orDefault[D](fd: F[D])(t: Timeout, default: => D): F[D]
}

object FutureCompletableSyntax {

  implicit class FutureCompletableOps[F[_],D](value: F[D]) {

    def orDefault(t: Timeout, default: => D)(implicit completableInstance: Completable[F]): F[D] = {
      completableInstance.orDefault[D](value)(t, default)
    }
  }
}

object CompletableInstances {
  implicit def tcCompletableInstUniqueName(implicit system : ActorSystem[Nothing], ec : ExecutionContext): Completable[Future] = new Completable[Future] {
    import akka.pattern._
    override def orDefault[D](fd: Future[D])(t: Timeout, default: => D): Future[D] = {
      val delayed = after(t.duration)(Future.successful(default))
      Future firstCompletedOf Seq(fd, delayed)
    }
  }
}
