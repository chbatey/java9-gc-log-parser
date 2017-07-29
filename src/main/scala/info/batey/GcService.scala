package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import info.batey.GCLogFileModel.TimeOffset
import info.batey.actors.GcStateActor.{GcState, GenerationSizes, HeapSize}
import info.batey.actors.{GcStateActor, PauseActor, UnknownLineEvent}

import scala.concurrent.ExecutionContext.Implicits._
import spray.json._

object GcService extends GcLogStream with GcStateJson {

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  override val log: LoggingAdapter = Logging(system, "main")

  override val young: ActorRef = system.actorOf(Props(classOf[PauseActor]), "YoungGen")
  override val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]), "UnknownMsgs")
  override val gcState: ActorRef = system.actorOf(Props(classOf[GcStateActor]), "GcState")

  def main(args: Array[String]): Unit = {
    fromGcLog
      .map(_.toJson)
      .toMat(Sink.foreach(println))(Keep.right)
      .run()
      .flatMap(_ => system.terminate())
  }
}

