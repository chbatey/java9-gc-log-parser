package info.batey

import info.batey.GCLogFileModel.{CollectionStats, PauseType, TimeOffset}

import scala.concurrent.duration.Duration

object GcStateModel {
  case class GcState(
        timeOffset: TimeOffset,
        fullGcs: Long,
        youngGcs: Long,
        initialMarks: Long,
        remarks: Long,
        mixed: Long,
        cleanups: Long,
        heapSize: HeapSize,
        generationSizes: GenerationSizes)

  case class HeapSize(size: Long, total: Long)

  // All gc events
  sealed trait GcEvent
  case class Pause(timeOffset: TimeOffset, gen: PauseType, dur: Duration, heapSize: HeapSizes, genSizes: GenerationSizes) extends GcEvent
  case class RemarkPause(timeOffset: TimeOffset, dur: Duration, heapSize: HeapSizes) extends GcEvent
  case class NotInteresting() extends GcEvent

  case class HeapSizes(before: Long, after: Long, total: Long)
  case class GenerationSizes(eden: Long, survivor: Long, old: Long, humongous: Long)

  object HeapSizes {
    def fromStats(stats: CollectionStats): HeapSizes = {
      HeapSizes(stats.before, stats.after, stats.total)
    }
  }

}
