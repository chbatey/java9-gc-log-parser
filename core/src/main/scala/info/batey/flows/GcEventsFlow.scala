package info.batey.flows

import akka.NotUsed
import akka.stream.scaladsl._
import info.batey.actors.GcStateActor.GcEvent
import info.batey.flows.CollectByGcEvent.{CollectedGcLines, LinesForPause, SingleLineInfo}

object GcEventsFlow {
  val flow: Flow[CollectedGcLines, GcEvent, NotUsed] =
    Flow[CollectedGcLines].map {
      case LinesForPause(lines) => ???
      case SingleLineInfo(line) => ???
    }
}
