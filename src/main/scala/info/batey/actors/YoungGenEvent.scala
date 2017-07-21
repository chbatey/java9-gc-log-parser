package info.batey.actors

import akka.event.Logging
import akka.persistence.PersistentActor
import info.batey.GcLineParser.YoungPause

class YoungGenEvent extends PersistentActor {

  val log = Logging(context.system, this)

  override def persistenceId: String = "young-gen-pauses"

  var totalPauses: Long = 0

  private def onEvent(e: YoungPause) = {
    log.debug("Saving event {}", e)
    totalPauses = totalPauses + e.stats.duration.toMicros
  }

  override def receiveRecover: Receive = {
    case e: YoungPause => onEvent(e)
  }

  override def receiveCommand: Receive = {
    case e: YoungPause =>
      persist(e) { e =>
        onEvent(e)
      }
    case "how much?" => sender() ! totalPauses
    case e@_ =>
      log.info("Ignoring event {}", e)
  }
}
