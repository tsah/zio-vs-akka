package cache

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

abstract class Cache[T](calc: () => Future[T], ttl: FiniteDuration) {
  def get: Future[T]
}