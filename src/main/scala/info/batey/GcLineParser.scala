package info.batey

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.parsing.combinator.JavaTokenParsers

object GcLineParser extends JavaTokenParsers {
  sealed trait Level
  case object Info extends Level
  case object Warn extends Level

  sealed trait Generation
  case object Young extends Generation

  case class TimeOffset(l: Long) extends AnyVal

  object TimeOffset {
    implicit def toOffset(l: Long): TimeOffset = TimeOffset(l)
  }

  case class Metadata(offset: TimeOffset, level: Level)

  sealed trait G1GcEvent {
    val metadata: Metadata
  }

  case class GcInfo(metadata: Metadata, msg: String) extends G1GcEvent
  case class EvacuationPause(metadata: Metadata, gen: Generation, stats: CollectionStats) extends G1GcEvent
  case class InitialMark(metadata: Metadata, stats: CollectionStats) extends G1GcEvent
  case class ConcurrentCycle(metadata: Metadata) extends G1GcEvent
  case class Remark(metadata: Metadata, stats: CollectionStats) extends G1GcEvent

  case class CollectionStats(before: Long, after: Long, total: Long, duration: Duration)

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

  def initialMark: Parser[InitialMark] = header ~ "Pause Initial Mark (G1 Evacuation Pause)" ~ collectionStats ^^ {
    case meta ~ _ ~ stats => InitialMark(meta, stats)
  }

  def infoLine: Parser[GcInfo] = header ~ msg ^^ {
    case meta ~ msg => GcInfo(meta, msg)
  }

  def evacuationPause: Parser[EvacuationPause] = header ~ "Pause Young (G1 Evacuation Pause) " ~ collectionStats ^^ {
    case meta ~  _ ~ collectionStats => EvacuationPause(meta, Young, collectionStats)
  }

  def concurrentCycle: Parser[ConcurrentCycle] = header ~ "Concurrent Cycle" ^^ {
    case meta ~ _ => ConcurrentCycle(meta)
  }

  def remark: Parser[Remark] = header ~ "Pause Remark" ~ collectionStats ^^ {
    case meta ~ _ ~ stats => Remark(meta, stats)
  }

  def gcLine: Parser[G1GcEvent] = remark | evacuationPause | initialMark | concurrentCycle | infoLine
}
