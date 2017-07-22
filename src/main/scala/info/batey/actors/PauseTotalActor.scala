package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GC._
import info.batey.actors.PauseTotalActor.{TotalPause, TotalPauseQuery}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

class PauseTotalActor extends Actor {

  val log = Logging(context.system, this)

  var totalPauses: Long = 0

  def receive: Receive = {
    case e@G1GcEvent(_, Pause(_, CollectionStats(_, _,_, dur), _)) =>
      log.debug("Mixed msg {}", e)
      totalPauses += dur.toMicros
    case TotalPauseQuery =>
      sender ! TotalPause(totalPauses)
    case m@_ => log.info("Ignoring {}", m)

  }
}

object PauseTotalActor {
  case object TotalPauseQuery
  case class TotalPause(micros: Long)
}

