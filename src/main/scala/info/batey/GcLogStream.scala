package info.batey

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl._

import scala.concurrent.duration._

trait GcLogStream {

  import GcLineParser._

  implicit val system: ActorSystem
  implicit val materialiser: ActorMaterializer

  val youngGen: ActorRef
  val mixedMsgs: ActorRef

  val source: Source[String, NotUsed] = FileTailSource
    .lines(Paths.get("gc.log.0"), 1024, 1 second)

  val gcEvents: Source[G1GcEvent, NotUsed] =
    source.map(line => parse(gcLine, line).get)

  lazy val process: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val outlet: Outlet[G1GcEvent] = builder.add(gcEvents).out
    val generations = builder.add(Broadcast[G1GcEvent](3))

    val youngFilter = Flow[G1GcEvent].filter((evt: G1GcEvent) => evt.gen == Young)
    val mixedFilter = Flow[G1GcEvent].filter((evt: G1GcEvent) => evt.gen == Mixed)
    val unknownLine = Flow[G1GcEvent].filter(_.isInstanceOf[UnknownLine])

    outlet ~> generations
    generations ~> youngFilter ~> Sink.actorRef(youngGen, "done")
    generations ~> mixedFilter ~> Sink.actorRef(mixedMsgs, "done")
    generations ~> unknownLine ~> Sink.foreach(println)

    ClosedShape
  })
}
