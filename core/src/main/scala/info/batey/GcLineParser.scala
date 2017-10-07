package info.batey

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scala.util.parsing.combinator.JavaTokenParsers

object GcLineParser extends JavaTokenParsers {

  import GCLogFileModel._
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

  def tag: Parser[Tag] = "(gc|start|heap|phases)".r ~ opt(",") ^^ {
    case "gc" ~ _ => Gc
    case "heap" ~ _ => Heap
    case "phases" ~ _ => Phases
    case "start" ~ _ => Start
  }

  def header: Parser[Metadata] = "[" ~ offset ~ "]" ~ level ~ "[" ~ (tag+) ~ "]" ~ opt(eventId) ^^ {
    case _ ~ ts ~ _ ~ lvl ~ _ ~ tags ~ _  ~ _ => Metadata(ts.toMillis, lvl, tags.toSet)
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

  def pause: Parser[PauseEnd] = "Pause" ~ pauseType ~ opt(reason) ~ collectionStats ^^ {
    case _ ~ pauseType ~ reason ~ collectionStats => PauseEnd(pauseType, collectionStats, reason)
  }

  def concurrentCycle: Parser[LineDesc] = "Concurrent Cycle".r ^^ (_ => ConcurrentCycle)
  def tooSpace: Parser[LineDesc] = "To-space exhausted".r ^^ (_ => ToSpaceExhausted)

  def usingG1: Parser[LineDesc] = "Using G1".r ^^ { _ => UsingG1}

  def regionSize: Parser[RegionSize] = "Heap region size: " ~ wholeNumber ~ "M" ^^ {
    case _ ~ num ~ _ => RegionSize(num.toLong)
  }

  def heapEvent: Parser[HeapInfo] = regionSize

  def phases: Parser[Phase] = "[a-zA-Z ]*".r ~ ":" ~ offset ^^ {
    case str ~ _ ~ offset => Phase(str, offset)
  }

  def pauseStart: Parser[PauseStart] = "Pause" ~ pauseType ~ opt(reason) ^^ {
    case _ ~ pauseType ~ reason => PauseStart(pauseType, reason)
  }

  def region: Parser[Region] = "(Eden|Survivor|Old|Humongous)".r ^^ {
    case "Eden" => Eden
    case "Survivor" => Survivor
    case "Old" => Old
    case "Humongous" => Humongous
  }

  def nrInParen: Parser[Long] = "(" ~ wholeNumber ~ ")" ^^ {
    case _ ~ nr ~ _ => nr.toLong
  }

  def nrRegions: Parser[NrRegions] = region ~ "regions:" ~ wholeNumber ~ "->" ~ wholeNumber ~ opt(nrInParen) ^^ {
    case region ~ _ ~ before ~ _ ~ after ~ _ => NrRegions(region, before.toLong, after.toLong)
  }

  def lineDesc: Parser[LineDesc] = nrRegions | pause | pauseStart | concurrentCycle | tooSpace | usingG1 | heapEvent | phases

  def gcLine: Parser[G1GcLine] = header ~ lineDesc ^^ {
    case meta ~ eventType => G1GcLine(meta, eventType)
  }

  def unknown: Parser[Line] = ".*".r ^^ {
    UnknownLine
  }

  def gcParser: Parser[Line] = gcLine | unknown
}
