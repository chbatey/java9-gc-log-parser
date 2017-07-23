package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import info.batey.actors.GcStateActor.{GcState, HeapSize}
import info.batey.actors.{GcStateActor, OldGenerationActor, UnknownLineEvent, YoungGenerationActor}
import spray.json._

object GcService extends GcLogStream with DefaultJsonProtocol {

  // todo remove these once we remove the foreach Sink
  implicit val hsFormat: RootJsonFormat[HeapSize] = jsonFormat2(HeapSize)
  implicit val gsFormat: RootJsonFormat[GcState] = jsonFormat3(GcState)

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  val log = Logging(system, "main")

  override val young: ActorRef = system.actorOf(Props(classOf[YoungGenerationActor]), "YoungGen")
  override val full: ActorRef = system.actorOf(Props(classOf[OldGenerationActor]), "Tenured")
  override val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]), "UnknownMsgs")
  override val gcState: ActorRef = system.actorOf(Props(classOf[GcStateActor]), "GcState")

  val mode = "console"

  def main(args: Array[String]): Unit = {
    // todo cmd line args
    // todo in http mode we need to create the actors per request
    val consoleMode = process.toMat(Sink.foreach(gs => println(gs.toJson)))(Keep.right)
    consoleMode.run()
    //todo open tsdb and prometheus sinks!
  }
}

