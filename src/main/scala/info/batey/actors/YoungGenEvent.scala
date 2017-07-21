package info.batey.actors

import akka.event.Logging
import akka.persistence.PersistentActor
import info.batey.GC._

class YoungGenEvent extends PersistentActor {

  val log = Logging(context.system, this)

  override def persistenceId: String = "young-gen-pauses"

  var totalPauses: Long = 0

  private def onEvent(e: G1GcEvent): Unit = e match {
    case G1GcEvent(_, Pause(_, CollectionStats(_, _,_, dur), _)) => {
      log.debug("Saving event {}", e)
      totalPauses = totalPauses + dur.toMicros
    }
  }

  override def receiveRecover: Receive = {
    case e: G1GcEvent => onEvent(e)
  }

  override def receiveCommand: Receive = {
    case e: G1GcEvent =>
      persist(e) { e =>
        onEvent(e)
      }
    case "how much?" => sender() ! totalPauses
    case e@_ =>
      log.info("Ignoring event {}", e)
  }
}
