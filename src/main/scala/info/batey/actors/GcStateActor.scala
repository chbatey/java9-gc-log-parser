package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.actors.GcStateActor.GcState
import info.batey.actors.PauseTotalActor.StwPause

class GcStateActor extends Actor {

  private var log = Logging(context.system, this)

  var totalStwPauses: Long = 0

  def receive: Receive = {
    case StwPause(dur) =>
      log.debug(s"STW: $dur")
      totalStwPauses += dur
      sender ! GcState(totalStwPauses)
  }
}

object GcStateActor {
  case class GcState(totalStw: Long)
}
