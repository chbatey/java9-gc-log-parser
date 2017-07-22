package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import info.batey.actors.{GcStateActor, PauseTotalActor, UnknownLineEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object GcService extends HttpFrontEnd with GcLogStream {

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val log = Logging(system, "main")

  override val young: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val mixed: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val full: ActorRef = system.actorOf(Props(classOf[PauseTotalActor]))
  override val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]))
  override val gcState: ActorRef = system.actorOf(Props(classOf[GcStateActor]))

  val mode = "console"

  def main(args: Array[String]): Unit = {
    // todo cmd line args
    // todo in http mode we need to create the actors per request
    if (mode == "http") {
      val bound = Http().bindAndHandle(route, "localhost", 9090)
      log.info("Listing on port {}", 9090)
      StdIn.readLine()
      bound.flatMap(_.unbind()).onComplete(_ => system.terminate())
    } else {
      val consoleMode = process.toMat(Sink.foreach(println))(Keep.right)
      consoleMode.run()
    }
    //todo open tsdb and prometheus sinks!
  }
}

