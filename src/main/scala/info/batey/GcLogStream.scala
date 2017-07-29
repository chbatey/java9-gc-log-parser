package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._
import akka.util.Timeout
import info.batey.GCLogFileModel._
import info.batey.GcService.system
import info.batey.actors.GcStateActor.{GcEvent, GcState}
import info.batey.actors.{GcStateActor, PauseActor, UnknownLineEvent}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

object GcLogStream {
  def create()(implicit system: ActorSystem): GcLogStream = new GcLogStream()
}

class GcLogStream(implicit system: ActorSystem) {
  import GcLineParser._

  val young: ActorRef = system.actorOf(Props(classOf[PauseActor]), "YoungGen")
  val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]), "UnknownMsgs")
  val gcState: ActorRef = system.actorOf(Props(classOf[GcStateActor]), "GcState")

  def fromFile(path: String): Source[GcState, NotUsed] =
    FileTailSource.lines(Paths.get(path), 1024, 1 second)
        .via(eventsFlow())

  def fromGcLog: Source[GcState, NotUsed] =
    fromFile("gc.log")

  private def eventsFlow(): Flow[String, GcState, NotUsed] =
    Flow[String]
      .map(parse(gcParser, _).get)
      .via(
        Flow.fromGraph(GraphDSL.create() { implicit builder =>
          import GraphDSL.Implicits._

          val fanFactor = 1
          val generations = builder.add(Broadcast[Line](fanFactor))
          val merge = builder.add(Merge[GcEvent](fanFactor))

          val supportedPauseTypes: Set[PauseType] = Set(Full, Young, InitialMark, Remark, Mixed)
          val youngFilter = Flow[Line].filter({
            case G1GcLine(_, PauseEnd(ty, _, _)) if supportedPauseTypes.contains(ty) => true
            case G1GcLine(_, PauseStart(ty, _)) if supportedPauseTypes.contains(ty) => true
            case G1GcLine(_, NrRegions(_, _, _)) => true
            case _ => false
          })

          val youngFlow = flowFromActor[Line, GcEvent](young)

          val gcStateFlow = flowFromActor[GcEvent, GcState](gcState)

          val end = builder.add(gcStateFlow)

          generations ~> youngFilter ~> youngFlow ~> merge
          // todo deal with un-parsed lines down a different flow
          //    val unknownLine: Flow[Line, Line, NotUsed] = Flow[Line].filter(_.isInstanceOf[UnknownLine])
          //    generations ~> unknownLine ~> merge
          merge ~> end
          FlowShape(generations.in, end.out)
        })
      )

  private def flowFromActor[From: ClassTag, To: ClassTag](actor: ActorRef): Flow[From, To, NotUsed] = {
    implicit val timeout = Timeout(1 second)

    val processor: From => Future[To] = (from: From) => {
      (actor ? from).mapTo[To]
    }
    Flow[From].mapAsync(1)(processor)
  }
}
