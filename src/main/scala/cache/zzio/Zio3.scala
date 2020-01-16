package cache.zzio

import cache.Cache
import zio.clock.Clock
import zio.duration.Duration
import zio._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Zio3 {

  class ZioCache[T](calc: () => Future[T], ttl: FiniteDuration) {

    def create: UIO[ZIO[Clock, Throwable, T]] = for {
      cachedVal <- RefM.make[Option[Promise[Throwable, T]]](None)
    } yield for {
      modificationResult <- cachedVal.modify {
        case v@Some(promise) => ZIO.succeed((promise.await, v))
        case None => for {
          promise <- Promise.make[Throwable, T]
          calcResult = calcNewVal(promise, cachedVal)
        } yield (calcResult, Some(promise))
      }
      eventualVal <- modificationResult
    } yield eventualVal

    private def calcNewVal(promise: Promise[Throwable, T], cachedVal: RefM[Option[Promise[Throwable, T]]]) = for {
      _ <- promise.complete(Task.fromFuture(_ => calc())).fork
      _ <- promise.await.either.flatMap {
        case Left(_) => cachedVal.set(None)
        case Right(_) => cachedVal.set(None).delay(Duration.fromScala(ttl))
      }.fork
      calcResult <- promise.await
    } yield calcResult
  }

  class ZioBackedCacheImpl[T](calc: () => Future[T], ttl: FiniteDuration) extends Cache[T](calc, ttl){

    private val runtime = new DefaultRuntime {}

    private val cache = runtime.unsafeRun(new ZioCache[T](calc, ttl).create)

    override def get: Future[T] = runtime.unsafeRunToFuture(cache)

  }
}
