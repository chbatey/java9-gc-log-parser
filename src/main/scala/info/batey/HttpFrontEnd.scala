package info.batey

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.Timeout
import info.batey.actors.GcStateActor.GcState
import info.batey.actors.PauseTotalActor.{StwPause, TotalPause}
import spray.json._

import scala.concurrent.duration._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val totalPause: RootJsonFormat[TotalPause] = jsonFormat1(TotalPause)
  implicit val pauseEvent: RootJsonFormat[StwPause] = jsonFormat1(StwPause)
  implicit val gc: RootJsonFormat[GcState] = jsonFormat1(GcState)
}

trait HttpFrontEnd extends JsonSupport {
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  val young: ActorRef
  val mixed: ActorRef
  val full: ActorRef
  val unknown: ActorRef

  val process: Source[GcState, NotUsed]

  val route: Route = {
    pathPrefix("static") {
      encodeResponse {
        getFromResourceDirectory("static")
      }
    } ~
      pathPrefix("stream") {
        path("pauses") {
          val s: Source[ServerSentEvent, NotUsed] = process
            .map(toSSE)
            .keepAlive(1 second, () => ServerSentEvent.heartbeat)
            .recover {
              case e: Throwable =>
                println(e)
                e.printStackTrace(System.out)
                ServerSentEvent(s"We had a problem")
            }
          complete(s)
        }
      }
  }

  def toSSE(pe: GcState): ServerSentEvent = {
    println(pe)
    ServerSentEvent(pe.toJson.prettyPrint)
  }
}
