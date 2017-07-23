package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GCLogFileModel._
import info.batey.actors.GcStateActor.{FullPause, GenSizings, NotInteresting, YoungPause}

import scala.language.postfixOps

class OldGenerationActor extends Actor {

  val log = Logging(context.system, this)

  var totalPauses: Long = 0

  def receive: Receive = {
    case e@G1GcEvent(_, Pause(Full, CollectionStats(before, after, total, dur), _)) =>
      log.debug("Full event {}", e)
      totalPauses += dur.toMicros
      sender ! FullPause(dur, GenSizings(before, after, total))
    case m@_ =>
      sender ! NotInteresting()
      log.warning("Unknown msg {}", m)
  }
}

object OldGenerationActor {
  case object TotalPauseQuery
  case class TotalPause(micros: Long)
}

