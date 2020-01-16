package cache.akkka

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import cache.Cache

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Akka1 {

  case object Get

  case class Set[T](t: T)

  class CacheActor[T](calc: () => Future[T]) extends Actor {

    override def receive: Receive = empty

    def empty: Receive = {
      case Get =>
        val s = sender()
        calc().map { t =>
          self ! Set(t)
          s ! t
        }
      case Set(t: T) =>
        context.become(full(t))
    }

    def full(t: T): Receive = {
      case Get =>
        sender() ! t
      case _ =>
    }
  }

  object CacheActorProps {
    def props[T](calc: () => Future[T]): Props = Props(new CacheActor[T](calc))
  }

  class ActorBackedCacheImpl[T](calc: () => Future[T], ttl: FiniteDuration, actorSystem: ActorSystem) extends Cache[T](calc, ttl) {
    private val cacheActor = actorSystem.actorOf(CacheActorProps.props(calc))
    implicit private val timeout: Timeout = Timeout(10.minutes)

    override def get: Future[T] = (cacheActor ? Get).asInstanceOf[Future[T]]
  }

}