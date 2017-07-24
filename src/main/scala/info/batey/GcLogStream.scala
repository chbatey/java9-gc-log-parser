package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._
import akka.util.Timeout
import info.batey.GCLogFileModel._
import info.batey.actors.GcStateActor.{GcEvent, GcState}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

trait GcLogStream {

  import GcLineParser._

  implicit val system: ActorSystem
  implicit val materialiser: ActorMaterializer

  val log: LoggingAdapter

  val young: ActorRef
  val gcState: ActorRef
  val unknown: ActorRef

  val source: Source[String, NotUsed] = FileTailSource
    .lines(Paths.get("gc.log"), 1024, 1 second)

  val gcEvents: Source[Line, NotUsed] =
    source.map(line => parse(gcParser, line).get)
      .map(msg => {
        log.debug("{}", msg)
        msg
      })

  // todo turn this into a flow from Line => GcState and re-use for batch processing
  // and a streaming web request
  lazy val process: Source[GcState, NotUsed] = Source.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val fanFactor = 1
    val outlet = builder.add(gcEvents).out
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

    outlet ~> generations
    generations ~> youngFilter ~> youngFlow ~> merge
    // todo deal with un-parsed lines down a different flow
    //    val unknownLine: Flow[Line, Line, NotUsed] = Flow[Line].filter(_.isInstanceOf[UnknownLine])
    //    generations ~> unknownLine ~> merge
    merge ~> end
    SourceShape(end.out)
  })


  private def flowFromActor[From: ClassTag, To: ClassTag](actor: ActorRef): Flow[From, To, NotUsed] = {
    implicit val timeout = Timeout(1 second)

    val processor: From => Future[To] = (from: From) => {
      (actor ? from).mapTo[To]
    }
    Flow[From].mapAsync(1)(processor)
  }
}
