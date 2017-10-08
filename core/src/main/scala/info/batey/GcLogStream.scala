package info.batey

import java.nio.file.Paths
import java.util.UUID

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._
import akka.util.{ByteString, Timeout}
import info.batey.GCLogFileModel._
import GcStateModel.{GcEvent, GcState}
import info.batey.flows.{CollectPauseLines, GcStateFlow}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

object GcLogStream {
  def create()(implicit system: ActorSystem): GcLogStream = new GcLogStream()
}

class GcLogStream(implicit system: ActorSystem) {

  import GcLineParser._

  def oneOffFromFile(path: String): Source[GcState, _] = {
    println(s"Running with file: $path")
     FileIO.fromPath(Paths.get(path))
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(_.utf8String)
      .via(eventsFlow())
  }

  def tailFile(path: String): Source[GcState, NotUsed] =
    FileTailSource.lines(Paths.get(path), 1024, 1 second)
      .via(eventsFlow())

  private def eventsFlow(): Flow[String, GcState, NotUsed] = {
    Flow[String]
      .map(parse(gcParser, _).get)
      .via(
        Flow.fromGraph(GraphDSL.create() { implicit builder =>
          import GraphDSL.Implicits._

          val fanFactor = 1
          val generations = builder.add(Broadcast[Line](fanFactor))
          val merge = builder.add(Merge[GcEvent](fanFactor))

          val supportedPauseTypes: Set[PauseType] = Set(Full, Young, InitialMark, Remark, Mixed)
          val pausesFilter = Flow[Line].filter({
            case G1GcLine(_, PauseEnd(ty, _, _)) if supportedPauseTypes.contains(ty) => true
            case G1GcLine(_, PauseStart(ty, _)) if supportedPauseTypes.contains(ty) => true
            case G1GcLine(_, NrRegions(_, _, _)) => true
            case _ => false
          })

          val pauseCollector: Flow[Line, GcEvent, NotUsed] = CollectPauseLines.CollectByGcEvent
          val gcStateFlow: Flow[GcEvent, GcState, NotUsed] = GcStateFlow.GcStateFlow
          val end: FlowShape[GcEvent, GcState] = builder.add(gcStateFlow)

          generations ~> pausesFilter ~> pauseCollector ~> merge
          // todo deal with un-parsed lines down a different route
          merge ~> end
          FlowShape(generations.in, end.out)
        })
      )
  }
}
