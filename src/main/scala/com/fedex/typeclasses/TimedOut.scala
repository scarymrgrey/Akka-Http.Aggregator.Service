package com.fedex.typeclasses


import akka.actor.typed.ActorSystem
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

trait TimedOut[F[_]] {
  def orDefault[D](fd: F[D])(t: Timeout, default: => D): F[D]
}

object TimedOutSyntax {
  implicit class TimedOutOps[F[_], D](value: F[D]) {
    def setTimeOut(t: Timeout, default: => D)(implicit completableInstance: TimedOut[F]): F[D] = {
      completableInstance.orDefault[D](value)(t, default)
    }
  }
}

object TimedOutInstances {
  implicit def tcCompletableInstUniqueName(implicit system: ActorSystem[Nothing], ec: ExecutionContext): TimedOut[Future] = new TimedOut[Future] {

    import akka.pattern._

    override def orDefault[D](fd: Future[D])(t: Timeout, default: => D): Future[D] = {
      val delayed = after(t.duration)(Future.successful(default))
      Future firstCompletedOf Seq(fd, delayed)
    }
  }
}





