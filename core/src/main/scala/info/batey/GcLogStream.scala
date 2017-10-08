package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._
import akka.util.ByteString
import info.batey.GCLogFileModel._
import info.batey.GcStateModel.{GcEvent, GcState}
import info.batey.flows.{CollectPauseLines, GcStateFlow}

import scala.concurrent.duration._
import scala.language.postfixOps


object GcLogStream {

  import GcLineParser._

  private val supportedPauseTypes: Set[PauseType] = Set(Full, Young, InitialMark, Remark, Mixed)
  private val supportedTypesFilter: Flow[Line, Line, NotUsed] = Flow[Line].filter({
    case G1GcLine(_, PauseEnd(ty, _, _)) if supportedPauseTypes.contains(ty) => true
    case G1GcLine(_, PauseStart(ty, _)) if supportedPauseTypes.contains(ty) => true
    case G1GcLine(_, NrRegions(_, _, _)) => true
    case _ => false
  })

  def oneOffStateSource(path: String): Source[GcState, NotUsed] = {
    FileIO.fromPath(Paths.get(path))
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(_.utf8String)
      .via(stateFlow)
      .mapMaterializedValue(_ => NotUsed)
  }

  def oneOffEventSource(path: String): Source[GcEvent, NotUsed] = {
    FileIO.fromPath(Paths.get(path))
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(_.utf8String)
      .via(eventsFlow)
      .mapMaterializedValue(_ => NotUsed)
  }

  def liveStateSource(path: String): Source[GcState, NotUsed] =
    FileTailSource.lines(Paths.get(path), 1024, 1 second)
      .via(stateFlow)

  def liveEventSource(path: String): Source[GcEvent, NotUsed] =
    FileTailSource.lines(Paths.get(path), 1024, 1 second)
      .via(eventsFlow)

  val eventsFlow: Flow[String, GcEvent, NotUsed] =
    Flow[String]
      .map(parse(gcParser, _).get)
      .via(supportedTypesFilter)
      .via(CollectPauseLines.CollectByGcEvent)

  val stateFlow: Flow[String, GcState, NotUsed] =
    Flow[String]
      .via(eventsFlow)
      .via(GcStateFlow.GcStateFlow)
}
