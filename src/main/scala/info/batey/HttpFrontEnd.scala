package info.batey

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.ExecutionContext.Implicits.global

trait HttpFrontEnd {
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  val youngGen: ActorRef
  val mixedMsgs: ActorRef

  val route: Route = {
    path("young") {
      get {
        val totalPauseTime = (youngGen ? "how much?").mapTo[Long].map(l => s"$l microseconds")
        complete(totalPauseTime)
      }
    } ~
      path("mixed") {
        get {
          val totalPauseTime = (mixedMsgs ? "how much?").mapTo[Long].map(l => s"$l microseconds")
          complete(totalPauseTime)
        }
      }
  }
}
