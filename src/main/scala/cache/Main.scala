package cache

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import cache.akkka.{Akka3, Akka1, Akka2}
import zio.{ZIO, console}
import zio.console.Console

import scala.concurrent.duration._
import scala.util.Random

object CacheHelper {

  def printWithTime(s: String): Unit = {
    println(((System.currentTimeMillis()/ 1000) % 1000).toString + " " + s)
  }

  def heavyCalc: () => Future[String] = () => Future {
    printWithTime("calculating")
    Thread.sleep(2000)
    "Hello"
  }

  def heavyUnreliableCalc: () => Future[String] = () => Future {
    printWithTime("calculating")
    Thread.sleep(1000)
    if (Random.nextDouble() > 0.1) {
      "Success"
    } else {
      printWithTime("Failed")
      throw new RuntimeException
    }
  }

  val ttl: FiniteDuration = 4.seconds

}

object AkkaHelper {
  val actorSystem: ActorSystem = ActorSystem("cache")
}

object NaiveAkkaMain extends App {
  val stringCache = new Akka1.ActorBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl, AkkaHelper.actorSystem)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(println)
  }
}

object NaiveAkkaWithTTLMain extends App {
  val stringCache = new Akka2.ActorBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl, AkkaHelper.actorSystem)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
}

object CorrectAkkaMain extends App {
  val stringCache = new Akka3.ActorBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl, AkkaHelper.actorSystem)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
}

object NaiveZioMain extends App {
  val stringCache = new zzio.Zio1.ZioBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
}

object NaiveZioWithTTLMain extends App {
  val stringCache = new zzio.Zio2.ZioBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
  Thread.sleep(5000)
}

object CorrectZioMain extends App {
  val stringCache = new zzio.Zio3.ZioBackedCacheImpl[String](CacheHelper.heavyCalc, CacheHelper.ttl)

  1 to 10 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
  Thread.sleep(5000)
}

object CorrectZioWithUnreliableCalcMain extends App {
  val stringCache = new zzio.Zio3.ZioBackedCacheImpl[String](CacheHelper.heavyUnreliableCalc, CacheHelper.ttl)

  1 to 100 foreach{ _ =>
    Thread.sleep(1000)
    stringCache.get.map(CacheHelper.printWithTime)
  }
  Thread.sleep(5000)
}


object Main extends zio.App {
  override def run(args: List[String]): ZIO[Console, Nothing, Int] = for {
    _ <- console.putStrLn("Hello, world")
    exitVal <-  ZIO.succeed(0)
  } yield exitVal
}
