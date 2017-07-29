package info.batey

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{Inlet, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.util.Timeout
import info.batey.actors.GcStateActor.{GcState, GenerationSizes, HeapSize}
import info.batey.actors.PauseActor.TotalPause
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

object HttpFrontEnd extends GcStateJson {
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  def routes(implicit logStream: GcLogStream): Route = cors() {
    pathPrefix("static") {
      encodeResponse {
        getFromResourceDirectory("static")
      }
    } ~
    pathPrefix("stream") {
      path("gc") {
        val pipeline = logStream.fromGcLog
          .map(_.toJson.prettyPrint)
          .map(ServerSentEvent(_, Some("gc-event")))
          .keepAlive(1 second, () => ServerSentEvent.heartbeat)
          .recover {
            case e: Throwable =>
              println(e)
              e.printStackTrace(System.out)
              ServerSentEvent(e.getMessage(), Some("error"))
          }
        complete(pipeline)
      }
    }
  }
}
