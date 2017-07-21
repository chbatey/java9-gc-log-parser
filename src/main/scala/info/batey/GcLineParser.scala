package info.batey

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.parsing.combinator.JavaTokenParsers

object GcLineParser extends JavaTokenParsers {
  sealed trait Level
  case object Info extends Level
  case object Warn extends Level

  case class TimeOffset(l: Long) extends AnyVal

  object TimeOffset {
    implicit def toOffset(l: Long): TimeOffset = TimeOffset(l)
  }

  case class Metadata(offset: TimeOffset, level: Level = Info)

  sealed trait PauseType
  case object Young extends PauseType
  case object InitialMark extends PauseType
  case object Remark extends PauseType
  case object Mixed extends PauseType

  case class G1GcEvent(metadata: Metadata, event: EventType)

  sealed trait EventType
  case class Pause(which: PauseType, stats: CollectionStats, reason: Reason) extends EventType

  case class CollectionStats(before: Long, after: Long, total: Long, duration: Duration)

  sealed trait Reason
  case object HumongousAllocation extends Reason
  case object Evacuation extends Reason

  import TimeOffset._

  def timestamp: Parser[String] = "[" ~ floatingPointNumber ~ "s]" ^^ {
    case _ ~ t ~ _ => t
  }

  def level: Parser[Level] = "[" ~ "(info|warn)".r ~ "]" ^^ {
    case _ ~ "info" ~ _ => Info
    case _ ~ "warn" ~ _ => Warn
  }

  def duration: Parser[Duration] = floatingPointNumber ~ "(ms|s)".r ^^ {
    case time ~ "ms" => time.toDouble milliseconds
    case time ~ "s" => time.toDouble seconds
  }

  def header: Parser[Metadata] = "[" ~ duration ~ "]" ~ level ~ "[gc]" ~ opt(eventId) ^^ {
    case _ ~ ts ~ _ ~ lvl  ~ _ ~ _ => Metadata(ts.toMillis, lvl)
  }

  def collectionStats: Parser[CollectionStats] = wholeNumber ~ "M->" ~ wholeNumber ~ "M(" ~ wholeNumber ~ "M) " ~ duration ^^ {
    case before ~ _ ~ after ~ _ ~ total ~ _ ~ duration => CollectionStats(before.toLong, after.toLong, total.toLong, duration)
  }

  def eventId: Parser[Int] = "GC(" ~ wholeNumber ~ ")" ^^ {
    case _ ~ i ~ _ => i.toInt
  }

  def msg: Parser[String] = """.+""".r ^^ identity

//  def initialMark: Parser[InitialMark] = header ~ "Pause Initial Mark (G1 Evacuation Pause)" ~ collectionStats ^^ {
//    case meta ~ _ ~ stats => InitialMark(meta, stats, Evacuation)
//  }
//
//  def welconeMsg: Parser[GcInfo] = header ~ "Using G1" ^^ {
//    case meta ~ msg => GcInfo(meta, msg)
//  }
//
//  def evacuationPause: Parser[G1GcEvent] = header ~ "Pause" ~ "(Young|Mixed)".r ~ "(G1 Evacuation Pause) " ~ collectionStats ^^ {
//    case meta ~  _ ~ "Young" ~ _ ~ collectionStats => YoungPause(meta, collectionStats, Evacuation)
//    case meta ~  _ ~ "Mixed" ~ _ ~ collectionStats => MixedPause(meta, collectionStats, Evacuation)
//  }
//
//  def concurrentCycle: Parser[ConcurrentCycle] = header ~ "Concurrent Cycle" ^^ {
//    case meta ~ _ => ConcurrentCycle(meta)
//  }
//
//  def remark: Parser[Remark] = header ~ "Pause Remark" ~ collectionStats ^^ {
//    case meta ~ _ ~ stats => Remark(meta, stats)
//  }

//  def cleanup: Parser[Cleanup] = header ~ "Pause Cleanup" ~ collectionStats ^^ {
//    case meta ~ _ ~ stats => Cleanup(meta, stats)
//  }
//
//  def humongous: Parser[InitialMark] = header ~ "Pause Initial Mark (G1 Humongous Allocation)" ~ collectionStats ^^ {
//    case meta ~ _ ~ stats => InitialMark(meta, stats, HumongousAllocation)
//  }

//  def unknownLine: Parser[UnknownLine] = """.*""".r ^^ {
//    case e@_ => UnknownLine(null, NA, e)
//  }

  def pause(): Parser[Pause] = ???

  def eventType(): Parser[EventType] = ???

  def gcLine: Parser[G1GcEvent] = header ~ eventType ^^ {
    case meta ~ eventType => G1GcEvent(meta, eventType)
  }
}
