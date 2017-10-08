package info.batey.flows

import akka.NotUsed
import akka.stream.scaladsl._
import com.typesafe.scalalogging.LazyLogging
import info.batey.GCLogFileModel._
import info.batey.GcStateModel._

import scala.collection.immutable

object GcStateFlow extends LazyLogging {

  val GcStateFlow: Flow[GcEvent, GcState, NotUsed] = Flow[GcEvent].statefulMapConcat(() => {
    var gcState: GcState = GcState(0, 0, 0, 0, 0, 0, 0, HeapSize(0, 0), GenerationSizes(0, 0, 0, 0))

    (event: GcEvent) => {
      gcState = event match {
        case Pause(offset, Young, _, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, youngGcs = gcState.youngGcs + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case Pause(offset, InitialMark, _, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, initialMarks = gcState.initialMarks + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case Pause(offset, Full, _, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, fullGcs = gcState.fullGcs + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case Pause(offset, Mixed, _, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, mixed = gcState.mixed + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case Pause(offset, Cleanup, _, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, cleanups = gcState.cleanups + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case RemarkPause(offset, _, HeapSizes(_, after, total)) =>
          gcState.copy(timeOffset = offset, remarks = gcState.remarks + 1, heapSize = HeapSize(after, total))
        case e =>
          logger.warn("Unsupported GcEvent. Please raise a PR: {}", e)
          gcState
      }
      immutable.Iterable(gcState)
    }
  })

}
