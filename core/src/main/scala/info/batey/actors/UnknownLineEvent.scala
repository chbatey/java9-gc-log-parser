package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GCLogFileModel.UnknownLine
import info.batey.actors.UnknownLineEvent.GetUnknownLines

class UnknownLineEvent extends Actor {
  private val log = Logging(context.system, this)

  var unknownLines: Long = 0

  def receive: Receive = {
    case UnknownLine(line) =>
      unknownLines += 1
      log.warning("Line unparsed, please report bug: {}", line)
    case GetUnknownLines =>
      sender() ! unknownLines
  }
}

object UnknownLineEvent {
  case object GetUnknownLines
}
