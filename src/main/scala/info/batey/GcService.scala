package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import info.batey.actors.{MixedGenEvent, YoungGenEvent}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.io.StdIn

object GcService extends HttpFrontEnd with GcLogStream {

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  override val youngGen: ActorRef = system.actorOf(Props(classOf[YoungGenEvent]))
  override val mixedMsgs: ActorRef = system.actorOf(Props(classOf[MixedGenEvent]))

  def main(args: Array[String]): Unit = {
    process.run()
    val bound = Http().bindAndHandle(route, "localhost", 9090)
    StdIn.readLine()
    bound.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}

