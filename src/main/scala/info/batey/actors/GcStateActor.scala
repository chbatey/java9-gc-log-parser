package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GCLogFileModel.{Full, InitialMark, PauseType, Young}
import info.batey.actors.GcStateActor._

import scala.concurrent.duration.Duration

class GcStateActor extends Actor {

  import context._

  private val log = Logging(context.system, this)

  def receive: Receive = gcState(GcState(0, 0, 0, HeapSize(0,0), 0, GenerationSizes(0, 0, 0, 0)))

  def gcState(gs: GcState): Receive = {
    case PauseDetails(pauseType, dur, HeapSizes(before, after, total), allocationRatePerMb, genSizes) =>
      val newState  = pauseType match {
        case Young =>
          gs.copy(youngGcs = gs.youngGcs + 1,
            heapSize = HeapSize(after, total),
            allocationRatePerMb = allocationRatePerMb,
            generationSizes = genSizes
          )
        case Full =>
          gs.copy(fullGcs = gs.fullGcs + 1,
            heapSize = HeapSize(after, total),
            allocationRatePerMb = allocationRatePerMb,
            generationSizes = genSizes
          )
        case InitialMark =>
          gs.copy(initialMarks = gs.initialMarks + 1,
            heapSize = HeapSize(after, total),
            allocationRatePerMb = allocationRatePerMb,
            generationSizes = genSizes
          )
        case ty@_ =>
          log.warning("Ignoring pause type: {}", ty)
          gs
      }

      sender ! newState
      become(gcState(newState))
    case NotInteresting() =>
      log.debug("Received a not interesting, go make it interesting!")
      sender ! gs
  }
}

object GcStateActor {
  // state
  case class GcState(
                      fullGcs: Long,
                      youngGcs: Long,
                      initialMarks: Long,
                      heapSize: HeapSize,
                      allocationRatePerMb: Double,
                      generationSizes: GenerationSizes
                    )

  case class HeapSize(size: Long, total: Long)


  // All gc events
  sealed trait GcEvent
  // shouldn't really use PauseType as it is the file model
  case class PauseDetails(gen: PauseType, dur: Duration, heapSize: HeapSizes, allocationRatePerMb: Double, genSizes: GenerationSizes) extends GcEvent
  case class NotInteresting() extends GcEvent

  case class HeapSizes(before: Long, after: Long, total: Long)
  case class GenerationSizes(eden: Long, survivor: Long, old: Long, humongous: Long)

}
