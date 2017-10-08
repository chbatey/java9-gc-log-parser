package info.batey.flows

import akka.NotUsed
import akka.stream.scaladsl._
import info.batey.GCLogFileModel._
import info.batey.GcStateModel._

import scala.collection.immutable

object GcStateFlow {

  val GcStateFlow: Flow[GcEvent, GcState, NotUsed] = Flow[GcEvent].statefulMapConcat(() => {
    var gcState: GcState = GcState(0, 0, 0, 0, 0, 0, 0, HeapSize(0, 0), GenerationSizes(0, 0, 0, 0))

    (event: GcEvent) => {
      gcState = event match {
        case DetailedPause(offset, Young, dur, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, youngGcs = gcState.youngGcs + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case DetailedPause(offset, InitialMark, dur, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, initialMarks = gcState.initialMarks + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case DetailedPause(offset, Full, dur, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, fullGcs = gcState.fullGcs + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case DetailedPause(offset, Mixed, dur, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, mixed = gcState.mixed + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case DetailedPause(offset, Cleanup, dur, HeapSizes(_, after, total), genSizes) =>
          gcState.copy(timeOffset = offset, cleanups = gcState.cleanups + 1, heapSize = HeapSize(after, total), generationSizes = genSizes)
        case RemarkPause(offset, dur, HeapSizes(_, after, total)) =>
          gcState.copy(timeOffset = offset, remarks = gcState.remarks + 1, heapSize = HeapSize(after, total))
      }
      immutable.Iterable(gcState)
    }
  })

}
