package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GCLogFileModel._
import info.batey.actors.GcStateActor._

import scala.language.postfixOps

class PauseActor extends Actor {
  import context._

  val log = Logging(context.system, this)

  var totalPauses: Long = 0
  var previousHeapSize: Long = 0
  var lastOffset: Long = 0

  private val basicPauses: Set[PauseType] = Set(Remark)

  def receive: Receive = awaitingPause()
  // Fsm??

  def awaitingPause(): Receive = {
    case G1GcLine(_, PauseStart(pauseType,_)) if !basicPauses.contains(pauseType) =>
      sender ! NotInteresting()
      become(awaitingEdenStats(pauseType))
    case G1GcLine(_, PauseStart(pauseType,_)) =>
      sender ! NotInteresting()
      become(awaitingBasicPause(pauseType))
    case _@msg =>
      // todo remove this once we handle all types of pauses
      sender ! NotInteresting()
      log.warning("1 {}", msg)
  }

  def awaitingEdenStats(pauseType: PauseType): Receive = {
    case G1GcLine(_, nr@NrRegions(Eden, _, _)) =>
      sender ! NotInteresting()
      become(awaitingSurvivorStats(pauseType, nr))
    case _@msg => log.warning("2 {}", msg)
  }

  def awaitingSurvivorStats(pauseType: PauseType, eden: NrRegions): Receive = {
    case G1GcLine(_, nr@NrRegions(Survivor, _, _)) =>
      sender ! NotInteresting()
      become(awaitingOldSTats(pauseType, eden, nr))
    case _@msg => log.warning("3 {}", msg)
  }

  def awaitingOldSTats(pauseType: PauseType,eden: NrRegions, surv: NrRegions): Receive = {
    case G1GcLine(_, nr@NrRegions(Old, _, _)) =>
      sender ! NotInteresting()
      become(awaitingHumongousStats(pauseType, eden, surv, nr))
    case _@msg => log.warning("4 {}", msg)
  }

  def awaitingHumongousStats(pauseType: PauseType, eden: NrRegions, surv: NrRegions, old: NrRegions): Receive = {
    case G1GcLine(_, nr@NrRegions(Humongous, _, _)) =>
      sender ! NotInteresting()
      become(awaitingEnd(pauseType, eden, surv, old, nr))
    case _@msg => log.warning("5 {}", msg)
  }

  def awaitingEnd(pauseType: PauseType, eden: NrRegions, surv: NrRegions, old: NrRegions, hum: NrRegions): Receive = {
    case G1GcLine(Metadata(offset,_, _), PauseEnd(pt, CollectionStats(before, after, total, dur), _)) if pauseType == pt => // bug if this guard does not match
      val totalAllocatedMB = before - previousHeapSize
      val timeBetweenCollectionMillis = offset.millis - lastOffset
      val allocationRate = (totalAllocatedMB / timeBetweenCollectionMillis.toDouble) * 1000

      log.debug("Allocated {} in {} milliseconds", totalAllocatedMB, timeBetweenCollectionMillis)
      log.debug("Allocation rate per second: {}", allocationRate)

      totalPauses += dur.toMicros
      sender ! DetailedPause(
        offset,
        pauseType,
        dur,
        HeapSizes(before, after, total),
        allocationRate,
        GenerationSizes(eden.after, surv.after, old.after, hum.after))

      previousHeapSize = after
      lastOffset = offset.millis
      become(awaitingPause())
    case _@msg => log.warning("6 {}", msg)
  }

  def awaitingBasicPause(pauseType: PauseType): Receive = {
     case G1GcLine(Metadata(offset,_, _), PauseEnd(pt, CollectionStats(before, after, total, dur), _)) if pauseType == pt => // bug if this guard does not match
      val totalAllocatedMB = before - previousHeapSize
      val timeBetweenCollectionMillis = offset.millis - lastOffset
      val allocationRate = (totalAllocatedMB / timeBetweenCollectionMillis.toDouble) * 1000

      log.debug("Allocated {} in {} milliseconds", totalAllocatedMB, timeBetweenCollectionMillis)
      log.debug("Allocation rate per second: {}", allocationRate)

      totalPauses += dur.toMicros
      sender ! BasicPause(
        offset,
        pauseType,
        dur,
        HeapSizes(before, after, total),
        allocationRate)

      previousHeapSize = after
      lastOffset = offset.millis
      become(awaitingPause())
    case _@msg => log.warning("6 {}", msg)
  }
}

object PauseActor {
  case object TotalPauseQuery
  case class TotalPause(micros: Long)
}

