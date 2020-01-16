package cache.zzio

import cache.Cache
import zio.clock.Clock
import zio.duration.Duration
import zio.{DefaultRuntime, Ref, Task, UIO, ZIO}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Zio2 {
  class ZioCache[T](calc: () => Future[T], ttl: FiniteDuration) {

    def create: UIO[ZIO[Clock, Throwable, T]] = for {
      cached <- Ref.make[Option[T]](None)
    } yield for {
      optionalVal <- cached.get
      actualVal <- optionalVal match {
        case Some(t) => Task.succeed(t)
        case None => Task.fromFuture(_ => calc())
      }
      _ <- cached.set(Some(actualVal))
      _ <- cached.set(None).delay(Duration.fromScala(ttl)).fork
    } yield actualVal
  }

  class ZioBackedCacheImpl[T](calc: () => Future[T], ttl: FiniteDuration) extends Cache[T](calc, ttl){

    private val runtime = new DefaultRuntime {}

    private val cache = runtime.unsafeRun(new ZioCache[T](calc, ttl).create)

    override def get: Future[T] = runtime.unsafeRunToFuture(cache)

  }
}
