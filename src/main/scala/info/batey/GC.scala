package info.batey

import scala.concurrent.duration.Duration

object GC {
  sealed trait Level
  case object Info extends Level
  case object Warn extends Level

  case class TimeOffset(l: Long) extends AnyVal

  object TimeOffset {
    implicit def toOffset(l: Long): TimeOffset = TimeOffset(l)
  }

  case class Metadata(offset: TimeOffset, level: Level = Info)

  sealed trait Line
  case class G1GcEvent(metadata: Metadata, event: EventDesc) extends Line
  case class UnknownLine(line: String) extends Line

  sealed trait EventDesc
  case class Pause(which: PauseType, stats: CollectionStats, reason: Option[Reason]) extends EventDesc
  case object ConcurrentCycle extends EventDesc
  case object UsingG1 extends EventDesc
  case object ToSpaceExhausted extends EventDesc

  sealed trait PauseType
  case object Young extends PauseType
  case object InitialMark extends PauseType
  case object Remark extends PauseType
  case object Mixed extends PauseType
  case object Cleanup extends PauseType
  case object Full extends PauseType

  case class CollectionStats(before: Long, after: Long, total: Long, duration: Duration)

  sealed trait Reason
  case object HumongousAllocation extends Reason
  case object Evacuation extends Reason
  case object AllocationFailure extends Reason
  case object NA extends Reason

}
