package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
//import info.batey.GcLineParser.MixedPause
/*
class MixedGenEvent extends Actor {

  val log = Logging(context.system, this)

  var totalPauses: Long = 0

  def receive: Receive = {
    case e: MixedPause =>
      log.debug("Mixed msg {}", e)
      totalPauses += e.stats.duration.toMicros
    case "how much?" => sender() ! totalPauses
    case m@_ => log.info("Ignoring {}", m)
  }
}

*/
