package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GC._

class MixedGenEvent extends Actor {

  val log = Logging(context.system, this)

  var totalPauses: Long = 0

  def receive: Receive = {
    case e@G1GcEvent(_, Pause(_, CollectionStats(_, _,_, dur), _)) =>
      log.debug("Mixed msg {}", e)
      totalPauses += dur.toMicros
    case "how much?" => sender() ! totalPauses
    case m@_ => log.info("Ignoring {}", m)
  }
}

