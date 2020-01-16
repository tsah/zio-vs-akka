package cache.zzio

import cache.Cache
import zio.{DefaultRuntime, Ref, Task, UIO}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Zio1 {

  class ZioCache[T](calc: () => Future[T]) {

    def create: UIO[Task[T]] = for {
      cached <- Ref.make[Option[T]](None)
    } yield for {
        optionalVal <- cached.get
        actualVal <- optionalVal match {
          case Some(t) => Task.succeed(t)
          case None => Task.fromFuture(_ => calc())
        }
        _ <- cached.set(Some(actualVal))
      } yield actualVal
  }

  class ZioBackedCacheImpl[T](calc: () => Future[T], ttl: FiniteDuration) extends Cache[T](calc, ttl) {

    private val runtime = new DefaultRuntime {}

    private val cache: Task[T] = runtime.unsafeRun(new ZioCache[T](calc).create)

    override def get: Future[T] = runtime.unsafeRunToFuture(cache)

  }

}

