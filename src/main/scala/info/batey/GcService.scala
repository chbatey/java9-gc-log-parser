package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import info.batey.GCLogFileModel.TimeOffset
import info.batey.actors.GcStateActor.{GcState, GenerationSizes, HeapSize}
import info.batey.actors.{GcStateActor, PauseActor, UnknownLineEvent}

import scala.concurrent.ExecutionContext.Implicits._
import spray.json._

import scala.io.StdIn

object GcService extends GcStateJson {
  implicit val system: ActorSystem = ActorSystem("GCParser")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, "main")

  def main(args: Array[String]): Unit = {
    implicit val logStream = GcLogStream.create()

    val host = "localhost"
    val port = 9090

    val httpBinding = for {
      httpBinding <- Http().bindAndHandle(HttpFrontEnd.routes, host, port)
      consoleRun <- logStream.fromGcLog
        .map(_.toJson)
        .toMat(Sink.foreach(println))(Keep.right)
        .run()
    } yield { httpBinding }

    println(s"Servig HTTP requests at http://${host}:${port}/stream/gc")
    println("Press ENTER to terminate")
    StdIn.readLine()
  }
}
