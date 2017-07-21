package info.batey

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.parsing.combinator.JavaTokenParsers

object GcLineParser extends JavaTokenParsers {

  import GC._
  import TimeOffset._

  def timestamp: Parser[String] = "[" ~ floatingPointNumber ~ "s]" ^^ {
    case _ ~ t ~ _ => t
  }

  def level: Parser[Level] = "[" ~ "(info|warn)".r ~ "]" ^^ {
    case _ ~ "info" ~ _ => Info
    case _ ~ "warn" ~ _ => Warn
  }

  def offset: Parser[Duration] = floatingPointNumber ~ "(ms|s)".r ^^ {
    case time ~ "ms" => time.toDouble milliseconds
    case time ~ "s" => time.toDouble seconds
  }

  def header: Parser[Metadata] = "[" ~ offset ~ "]" ~ level ~ "[gc]" ~ opt(eventId) ^^ {
    case _ ~ ts ~ _ ~ lvl ~ _ ~ _ => Metadata(ts.toMillis, lvl)
  }

  def collectionStats: Parser[CollectionStats] = wholeNumber ~ "M->" ~ wholeNumber ~ "M(" ~ wholeNumber ~ "M) " ~ offset ^^ {
    case before ~ _ ~ after ~ _ ~ total ~ _ ~ duration => CollectionStats(before.toLong, after.toLong, total.toLong, duration)
  }

  def eventId: Parser[Int] = "GC(" ~ wholeNumber ~ ")" ^^ {
    case _ ~ i ~ _ => i.toInt
  }

  def reason: Parser[Reason] = "(" ~ "([a-zA-Z0-9 ])*".r ~ ")" ^^ {
    case _ ~ "G1 Evacuation Pause" ~ _ => Evacuation
    case _ ~ "G1 Humongous Allocation" ~ _ => HumongousAllocation
    case _ ~ "Allocation Failure" ~ _ => AllocationFailure
  }

  def pauseType: Parser[PauseType] = "(Full|Mixed|Cleanup|Young|Initial Mark|Remark)".r ^^ {
    case "Mixed" => Mixed
    case "Cleanup" => Cleanup
    case "Young" => Young
    case "Initial Mark" => InitialMark
    case "Remark" => Remark
    case "Full" => Full
  }

  def pause: Parser[Pause] = "Pause" ~ pauseType ~ opt(reason) ~ collectionStats ^^ {
    case _ ~ pauseType ~ reason ~ collectionStats => Pause(pauseType, collectionStats, reason)
  }

  def concurrentCycle: Parser[EventDesc] = "Concurrent Cycle".r ^^ (_ => ConcurrentCycle)
  def tooSpace: Parser[EventDesc] = "To-space exhausted".r ^^ (_ => ToSpaceExhausted)

  def usingG1: Parser[EventDesc] = "Using G1".r ^^ {_ => UsingG1}

  def eventDesc: Parser[EventDesc] = pause | concurrentCycle | tooSpace | usingG1

  def gcLine: Parser[G1GcEvent] = header ~ eventDesc ^^ {
    case meta ~ eventType => G1GcEvent(meta, eventType)
  }

  def unknown: Parser[Line] = ".*".r ^^ {
    UnknownLine
  }

  def gcParser: Parser[Line] = gcLine | unknown
}
