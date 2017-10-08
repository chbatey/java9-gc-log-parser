package info.batey.flows

import akka.stream.scaladsl._
import info.batey.GCLogFileModel._

import scala.collection.immutable

object CollectByGcEvent {

  sealed trait CollectedGcLines
  final case class LinesForPause(lines: Seq[Line]) extends CollectedGcLines
  final case class SingleLineInfo(line: Line) extends CollectedGcLines

  val CollectByGcEvent =
    Flow[Line].statefulMapConcat[CollectedGcLines](() => {
      var currentId = -1
      var buffer = List.empty[Line]
      (line: Line) => {

        line match {
          case line@G1GcLine(Metadata(_, None, _, _), _) =>
            List.apply(SingleLineInfo(line))
          case line@G1GcLine(Metadata(_, Some(eventId), _, _), PauseStart(_, _)) =>
            buffer = List(line)
            currentId = eventId
            immutable.Iterable.empty[CollectedGcLines]
          case line@G1GcLine(Metadata(_, Some(eventId), _, _), PauseEnd(_, _, _)) =>
            immutable.Iterable(LinesForPause((line :: buffer).reverse))
          case line@G1GcLine(Metadata(_, Some(eventId), _, _), _) =>
            require(eventId == currentId)
            buffer = line :: buffer
            immutable.Iterable.empty[CollectedGcLines]
        }
      }
    })
}
