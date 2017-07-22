package info.batey

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.pattern.ask

import info.batey.actors.PauseTotalActor.{TotalPause, TotalPauseQuery}
import info.batey.actors.UnknownLineEvent.GetUnknownLines
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val totalPause: RootJsonFormat[TotalPause] = jsonFormat1(TotalPause)
}

trait HttpFrontEnd extends JsonSupport {
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  val young: ActorRef
  val mixed: ActorRef
  val full: ActorRef
  val unknown: ActorRef

  val route: Route = {
    pathPrefix("pause") {
      path("young") {
        get {
          val totalPauseTime = (young ? TotalPauseQuery).mapTo[TotalPause]
          complete(totalPauseTime)
        }
      } ~
        path("mixed") {
          get {
            val totalPauseTime = (mixed ? TotalPauseQuery).mapTo[TotalPause]
            complete(totalPauseTime)
          }
        } ~
        path("full") {
          get {
            val totalPauseTime = (full ? TotalPauseQuery).mapTo[TotalPause]
            complete(totalPauseTime)
          }
        }
    } ~
    path("unparsed") {
      get {
        complete((unknown ? GetUnknownLines).mapTo[Long].map(nr => s"""Unparsed lines: $nr"""))
      }
    }
  }
}
