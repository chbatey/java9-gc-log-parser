package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._

import scala.concurrent.duration._

import GC._

trait GcLogStream {

  import GcLineParser._

  implicit val system: ActorSystem
  implicit val materialiser: ActorMaterializer

  val youngGen: ActorRef
  val mixedMsgs: ActorRef

  val source: Source[String, NotUsed] = FileTailSource
    .lines(Paths.get("gc.log"), 1024, 1 second)

  val gcEvents: Source[Line, NotUsed] =
    source.map(line => parse(gcParser, line).get)

  lazy val process: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val outlet: Outlet[Line] = builder.add(gcEvents).out
    val generations = builder.add(Broadcast[Line](3))

    val youngFilter = Flow[Line].filter( {
      case G1GcEvent(_, Pause(Young, _, _)) => true
      case _ => false
    })

    val mixedFilter = Flow[Line].filter({
      case G1GcEvent(_, Pause(Mixed, _, _)) => true
      case _ => false
    })

    val unknownLine = Flow[Line].filter(_.isInstanceOf[UnknownLine])

    outlet ~> generations
    generations ~> youngFilter ~> Sink.actorRef(youngGen, "done")
    generations ~> mixedFilter ~> Sink.actorRef(mixedMsgs, "done")
    generations ~> unknownLine ~> Sink.foreach(println)

    ClosedShape
  })
}
