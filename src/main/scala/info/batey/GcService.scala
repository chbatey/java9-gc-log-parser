package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import info.batey.GC.UnknownLine
import info.batey.actors.{PauseTotalActor, UnknownLineEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object GcService extends HttpFrontEnd with GcLogStream {

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  override val young: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val mixed: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val full: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]))

  def main(args: Array[String]): Unit = {
    process.run()
    val bound = Http().bindAndHandle(route, "localhost", 9090)
    StdIn.readLine()
    bound.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}

