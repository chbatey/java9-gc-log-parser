package info.batey

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.io.StdIn

object GcService extends GcStateJson with HttpFrontEnd {
  implicit val system: ActorSystem = ActorSystem("GCParser")
  implicit val materialiser: ActorMaterializer = ActorMaterializer()
  implicit val log: LoggingAdapter = Logging(system, "main")

  def main(args: Array[String]): Unit = {
    implicit val logStream = GcLogStream.create()

    val host = "localhost"
    val port = 9090

    val consoleRun: Future[Done] = logStream.fromGcLog
      .map(_.toJson)
      .toMat(Sink.foreach(println))(Keep.right)
      .run()

    val httpBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, host, port)

    println(s"Servig HTTP requests at http://${host}:${port}/stream/gc")
    println("Press ENTER to terminate")
    StdIn.readLine()
    httpBinding.flatMap(_.unbind())
  }
}
