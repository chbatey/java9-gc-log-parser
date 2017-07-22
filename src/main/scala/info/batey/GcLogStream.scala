package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._

import scala.concurrent.duration._
import GC._
import akka.util.Timeout
import info.batey.actors.GcStateActor.GcState
import info.batey.actors.PauseTotalActor.PauseEvent

import scala.concurrent.Future
import scala.language.postfixOps
import scala.reflect.ClassTag

trait GcLogStream {

  import GcLineParser._

  implicit val system: ActorSystem
  implicit val materialiser: ActorMaterializer

  val young: ActorRef
  val mixed: ActorRef
  val full: ActorRef
  val gcState: ActorRef
  val unknown: ActorRef

  val source: Source[String, NotUsed] = FileTailSource
    .lines(Paths.get("gc.log"), 1024, 1 second)

  val gcEvents: Source[Line, NotUsed] =
    source.map(line => parse(gcParser, line).get)

  // todo turn this into a flow from Line => GcState and re-use for batch processing
  // and a streaming web request
  lazy val process: Source[GcState, NotUsed] = Source.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val fanFactor = 3
    val outlet = builder.add(gcEvents).out
    val generations = builder.add(Broadcast[Line](fanFactor))
    val merge = builder.add(Merge[PauseEvent](fanFactor))

    val youngFilter = filterForPauseType(Young)
    val mixedFilter = filterForPauseType(Mixed)
    val fullFilter = filterForPauseType(Full)

    val youngFlow = flowFromActor[Line, PauseEvent](young)
    val mixedFlow = flowFromActor[Line, PauseEvent](mixed)
    val oldFlow = flowFromActor[Line, PauseEvent](full)
    val gcStateFlow = flowFromActor[PauseEvent, GcState](gcState)

    val end = builder.add(gcStateFlow)

    outlet ~> generations
    generations ~> youngFilter ~> youngFlow ~> merge
    generations ~> mixedFilter ~> mixedFlow ~> merge
    generations ~> fullFilter ~> oldFlow ~> merge
    // todo deal with un-parsed lines down a different flow
    //    val unknownLine: Flow[Line, Line, NotUsed] = Flow[Line].filter(_.isInstanceOf[UnknownLine])
    //    generations ~> unknownLine ~> merge
    merge ~> end
    SourceShape(end.out)
  })

  private def filterForPauseType(pType: PauseType): Flow[Line, Line, NotUsed] = {
    Flow[Line].filter({
      case G1GcEvent(_, Pause(pt, _, _)) if pt == pType => true
      case _ => false
    })
  }

  private def flowFromActor[From: ClassTag, To: ClassTag](actor: ActorRef): Flow[From, To, NotUsed] = {
    implicit val timeout = Timeout(1 second)
    val processor: From => Future[To] = (from: From) => {
      (actor ? from).mapTo[To]
    }
    Flow[From].mapAsync(1)(processor)
  }
}
