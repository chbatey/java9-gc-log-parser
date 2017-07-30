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
    implicit val conf = Conf()


    val modes = (conf.outputMode, conf.streamMode)
    println("Running in " + modes)

    val end: Future[_] = modes match {
      case (ConsoleMode, OneShot) =>
        consoleOneShot
      case (ConsoleMode, Stream) =>
        consoleStream
      case (HttpMode, Stream) =>
        streamHttp
    }

    end.flatMap(_ => system.terminate())
  }

  def consoleOneShot(implicit logStream: GcLogStream, config: Conf): Future[Done] =
    logStream.oneOffFromFile(config.filePath)
      .map(_.toJson)
      .toMat(Sink.foreach(println))(Keep.right)
      .run()

  def consoleStream(implicit logStream: GcLogStream, config: Conf): Future[Done] =
    logStream.tailFile(config.filePath)
      .map(_.toJson)
      .toMat(Sink.foreach(println))(Keep.right)
      .run()

  def streamHttp(implicit logStream: GcLogStream, config: Conf): Future[Unit] = {
    val httpBinding = Http().bindAndHandle(routes, config.httpHost, config.httpPort)
    println(s"Servig HTTP requests at http://${config.httpHost}/${config.httpPort}/gc")
    println("Press ENTER to terminate")
    StdIn.readLine()
    httpBinding.flatMap(_.unbind())
  }
}
