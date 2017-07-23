package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.actors.GcStateActor._

import scala.concurrent.duration.Duration

class GcStateActor extends Actor {

  import context._

  private val log = Logging(context.system, this)

  def receive: Receive = gcState(GcState(0, 0, HeapSize(0,0)))

  def gcState(gs: GcState): Receive = {
    case FullPause(dur, GenSizings(before, after, total)) =>
      val newState = gs.copy(fullGcs = gs.fullGcs + 1, heapSize = HeapSize(after, total))
      sender ! newState
      become(gcState(newState))
    case YoungPause(dur, GenSizings(before, after, total)) =>
      val newState  = gs.copy(youngGcs = gs.youngGcs + 1, heapSize = HeapSize(after, total))
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
                      heapSize: HeapSize)

  case class HeapSize(size: Long, total: Long)


  // All gc events
  sealed trait GcEvent
  case class FullPause(dur: Duration, sizings: GenSizings) extends GcEvent
  case class YoungPause(dur: Duration, sizings: GenSizings) extends GcEvent
  case class GenerationSizeChange(gen: Generation, newSize: Long) extends GcEvent
  case class NotInteresting() extends GcEvent

  case class GenSizings(before: Long, after: Long, total: Long)

  sealed trait Generation
  case object Young extends Generation
  case object Tenured extends Generation
}
