package cache.akkka

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import cache.Cache

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.pattern.ask
import akka.pattern.pipe

object Akka3 {

  case object Get

  case class Set[T](t: T)

  case object Evict

  class CacheActor[T](calc: () => Future[T], ttl: FiniteDuration) extends Actor {

    override def receive: Receive = empty

    def empty: Receive = {
      case Get =>
        val s = sender()
        val eventualValue = calc()
        context.become(calculating(eventualValue))
        eventualValue.map { t =>
          self ! Set(t)
          s ! t
        }.recover { case _ =>
          self ! Evict
        }

      case _ =>
    }

    def calculating(eventualResult: Future[T]): Receive = {
      case Get =>
        eventualResult.pipeTo(sender())
      case Evict =>
        context.become(empty)
      case Set(t: T) =>
        context.system.scheduler.scheduleOnce(ttl, self, Evict)
        context.become(full(t))
    }

    def full(t: T): Receive = {
      case Get =>
        sender() ! t
      case Evict =>
        context.become(empty)
      case _ =>

    }
  }

  object CacheActorProps {
    def props[T](calc: () => Future[T], ttl: FiniteDuration): Props = Props(new CacheActor[T](calc, ttl))
  }

  class ActorBackedCacheImpl[T](calc: () => Future[T], ttl: FiniteDuration, actorSystem: ActorSystem) extends Cache[T](calc, ttl) {
    private val cacheActor = actorSystem.actorOf(CacheActorProps.props(calc, ttl))
    implicit private val timeout: Timeout = Timeout(10.minutes)

    override def get: Future[T] = (cacheActor ? Get).asInstanceOf[Future[T]]
  }

}
