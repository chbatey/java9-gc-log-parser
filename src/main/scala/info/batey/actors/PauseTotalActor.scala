package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GC._
import info.batey.actors.PauseTotalActor.{StwPause, TotalPause, TotalPauseQuery}

import scala.language.postfixOps

class PauseTotalActor extends Actor {

  val log = Logging(context.system, this)

  var totalPauses: Long = 0

  def receive: Receive = {
    case e@G1GcEvent(_, Pause(_, CollectionStats(_, _,_, dur), _)) =>
      log.debug("Pause event {}", e)
      totalPauses += dur.toMicros
      sender ! StwPause(dur.toMicros)
    case TotalPauseQuery =>
      sender ! TotalPause(totalPauses)
    case m@_ => log.info("Ignoring {}", m)

  }
}

object PauseTotalActor {
  case object TotalPauseQuery
  case class TotalPause(micros: Long)

  sealed trait PauseEvent
  case class StwPause(micros: Long) extends PauseEvent
  case object NotInteresting extends PauseEvent
}

