package info.batey.flows

import akka.NotUsed
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import info.batey.GCLogFileModel._
import info.batey.GcStateModel._

import scala.collection.immutable

object CollectPauseLines extends LazyLogging {

  val CollectByGcEvent: Flow[Line, GcEvent, NotUsed] =
    Flow[Line].statefulMapConcat[GcEvent](() => {
      var start: PauseStart = null
      var id: Int = -1
      var regions = Map[Region, NrRegions]()

      (line: Line) => {
        line match {
          case G1GcLine(Metadata(_, None, _, _), _) =>
            immutable.Iterable(NotInteresting())
          case G1GcLine(Metadata(_, Some(eventId), _, _), ps@PauseStart(_, _)) =>
            start = ps
            id = eventId
            immutable.Iterable.empty[GcEvent]
          case G1GcLine(Metadata(_, Some(_), _, _), nr@NrRegions(region, _, _)) =>
            regions += region -> nr
            immutable.Iterable.empty[GcEvent]
          case line@G1GcLine(Metadata(_, Some(eventId), _, _), PauseEnd(pauseType, CollectionStats(before, after, total, duration), _)) =>
            if (start == null)
              throw new RuntimeException(s"Received PauseEnd before PauseStart. Either a bug or invalid GC log. EventId: ${eventId}")

            val events = pauseType match {
              case Remark =>
                immutable.Iterable(RemarkPause(
                  line.metadata.offset,
                  duration,
                  HeapSizes(before, after, total)
                ))
              case _ =>
                if (!regions.contains(Eden) || !regions.contains(Survivor) || !regions.contains(Old) || !regions.contains(Humongous))
                  throw new RuntimeException(s"Received PauseEnd without receiving region information. Either a bug or invalid GC log. Regions: {${regions.keys.mkString(",")}}. EventId: ${id}")

                immutable.Iterable(Pause(
                  line.metadata.offset,
                  pauseType,
                  duration,
                  HeapSizes(before, after, total),
                  GenerationSizes(
                    regions(Eden).after,
                    regions(Survivor).after,
                    regions(Old).after,
                    regions(Humongous).after,
                  )
                ))
            }

            start = null
            regions = Map.empty
            id = -1
            events
          case l =>
            logger.warn("Line not supported yet, please raise a PR: {}", l)
            immutable.Iterable.empty[GcEvent]

        }

      }
    })
}
