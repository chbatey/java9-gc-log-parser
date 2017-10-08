package info.batey

import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object GCLogFileModel {
  sealed trait Level
  case object Debug extends Level
  case object Info extends Level
  case object Warn extends Level
  case object Unknown extends Level

  sealed trait Tag
  case object Gc extends Tag
  case object Phases extends Tag
  case object Heap extends Tag
  case object Start extends Tag
  case object Cpu extends Tag
  case object StringTable extends Tag
  case object Metaspace extends Tag
  case object Marking extends Tag
  case object Exit extends Tag
  case object UnknownTag extends Tag

  // todo remove defaults
  case class Metadata(offset: TimeOffset, eventId: Option[Int], level: Level = Info, tags: Set[Tag] = Set(Gc))

  sealed trait Line
  case class G1GcLine(metadata: Metadata, event: LineDesc) extends Line
  case class UnknownLine(line: String) extends Line

  sealed trait LineDesc
  case class PauseStart(which: PauseType, reason: Option[Reason]) extends LineDesc
  case class NrRegions(region: Region, before: Long, after: Long) extends LineDesc
  case class PauseEnd(which: PauseType, stats: CollectionStats, reason: Option[Reason]) extends LineDesc

  case object ConcurrentCycle extends LineDesc
  case object UsingG1 extends LineDesc
  case object ToSpaceExhausted extends LineDesc

  sealed trait HeapInfo extends LineDesc
  case class RegionSize(size: Long) extends HeapInfo

  sealed trait Region
  case object Eden extends Region
  case object Survivor extends Region
  case object Old extends Region
  case object Humongous extends Region


  // Could be broken into multiple cases
  case class Phase(details: String, time: Duration) extends LineDesc

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

  object TimeOffset {
    implicit def toOffset(l: Long): TimeOffset = TimeOffset(l)
    implicit def toOffset(l: Int): TimeOffset = TimeOffset(l)
  }
  case class TimeOffset(millis: Long) extends AnyVal
}
