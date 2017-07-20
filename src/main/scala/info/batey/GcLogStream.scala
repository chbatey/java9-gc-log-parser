package info.batey

import java.nio.file.FileSystems

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.Source

import scala.concurrent.duration._

object GcLogStream {

  def main(args: Array[String]): Unit = {

    import GcLineParser._

    implicit val system = ActorSystem("GCParser")
    implicit val materialiser = ActorMaterializer()

    val fs = FileSystems.getDefault
    val path = fs.getPath("gc.log")
    val source: Source[String, NotUsed] = FileTailSource.lines(path, 1024, 1 second)

    // todo deal with failure
    val parseThyLog: Source[G1GcEvent, NotUsed] = source.map(line => parse(gcLine, line).get)

    //    val ohSoParsed = parseThyLog.runForeach(println)
    //    ohSoParsed.onComplete(_ => system.terminate())

  }
}
