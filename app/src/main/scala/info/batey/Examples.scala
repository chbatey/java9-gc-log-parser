package info.batey

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Examples extends App {

  implicit val system = ActorSystem()
  implicit val materialiser = ActorMaterializer()

  /**
    * Emits a [[info.batey.GcStateModel.GcState]] each time a full pause is parsed (will be spread across
    * many lines).
    */
  println("State source")
  val stateSource: Future[Done] = GcLogStream.oneOffStateSource("gc-ga.log")
    .runForeach(println)

  stateSource.onComplete(println)
  Await.ready(stateSource, 5.seconds)

  /**
    * Emits a [[info.batey.GcStateModel.GcEvent]] e.g. [[info.batey.GcStateModel.Pause]] each time a full
    * event is parsed
    */
  println("Events source")
  val eventsSource = GcLogStream.oneOffEventSource("gc-ga.log")
    .runForeach(println)

  eventsSource.onComplete(println)
  Await.ready(eventsSource, 5.seconds)

  /**
    * Flow for GcState
    */
  println("State flow")
  val stateFlow = Source(exampleLog.split("\n").toList)
    .via(GcLogStream.stateFlow)
    .runForeach(println)

  stateFlow.onComplete(println)
  Await.ready(stateFlow, 5.seconds)

  /**
    * Flow for GCEvents if log lines come from else where e.g. over the network
    */
  println("Events flow")
  val eventsFlow = Source(exampleLog.split("\n").toList)
    .via(GcLogStream.eventsFlow)
      .runForeach(println)

  eventsFlow.onComplete(println)
  Await.ready(eventsFlow, 5.seconds)

  system.terminate()


  lazy val exampleLog =
    """
      |[0.008s][info][gc,heap] Heap region size: 2M
      |[0.028s][info][gc     ] Using G1
      |[0.028s][info][gc,heap,coops] Heap address: 0x00000005cba00000, size: 8006 MB, Compressed Oops mode: Zero based, Oop shift amount: 3
      |[6.374s][info][gc,start     ] GC(0) Pause Young (G1 Evacuation Pause)
      |[6.374s][info][gc,task      ] GC(0) Using 8 workers of 8 for evacuation
      |[6.384s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
      |[6.384s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 6.3ms
      |[6.384s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 3.1ms
      |[6.384s][info][gc,phases    ] GC(0)   Other: 0.3ms
      |[6.384s][info][gc,heap      ] GC(0) Eden regions: 12->0(10)
      |[6.384s][info][gc,heap      ] GC(0) Survivor regions: 0->2(2)
      |[6.384s][info][gc,heap      ] GC(0) Old regions: 0->3
      |[6.384s][info][gc,heap      ] GC(0) Humongous regions: 0->0
      |[6.384s][info][gc,metaspace ] GC(0) Metaspace: 10000K->10000K(1058816K)
      |[6.384s][info][gc           ] GC(0) Pause Young (G1 Evacuation Pause) 24M->8M(502M) 9.895ms
      |[6.384s][info][gc,cpu       ] GC(0) User=0.04s Sys=0.01s Real=0.01s
      |[21.321s][info][gc,start     ] GC(1) Pause Young (G1 Evacuation Pause)
      |[21.321s][info][gc,task      ] GC(1) Using 8 workers of 8 for evacuation
      |[21.336s][info][gc,phases    ] GC(1)   Pre Evacuate Collection Set: 0.0ms
      |[21.336s][info][gc,phases    ] GC(1)   Evacuate Collection Set: 14.4ms
      |[21.336s][info][gc,phases    ] GC(1)   Post Evacuate Collection Set: 0.4ms
      |[21.336s][info][gc,phases    ] GC(1)   Other: 0.3ms
      |[21.336s][info][gc,heap      ] GC(1) Eden regions: 10->0(16)
      |[21.336s][info][gc,heap      ] GC(1) Survivor regions: 2->2(2)
      |[21.336s][info][gc,heap      ] GC(1) Old regions: 3->12
      |[21.337s][info][gc,heap      ] GC(1) Humongous regions: 0->0
      |[21.337s][info][gc,metaspace ] GC(1) Metaspace: 10007K->10007K(1058816K)
      |[21.337s][info][gc           ] GC(1) Pause Young (G1 Evacuation Pause) 28M->26M(502M) 15.344ms
      |[21.337s][info][gc,cpu       ] GC(1) User=0.05s Sys=0.03s Real=0.02s
      |[33.648s][info][gc,start     ] GC(2) Pause Young (G1 Evacuation Pause)
      |[33.648s][info][gc,task      ] GC(2) Using 8 workers of 8 for evacuation
      |[33.667s][info][gc,phases    ] GC(2)   Pre Evacuate Collection Set: 0.0ms
      |[33.667s][info][gc,phases    ] GC(2)   Evacuate Collection Set: 17.8ms
      |[33.667s][info][gc,phases    ] GC(2)   Post Evacuate Collection Set: 0.4ms
      |[33.667s][info][gc,phases    ] GC(2)   Other: 0.3ms
      |[33.667s][info][gc,heap      ] GC(2) Eden regions: 16->0(18)
      |[33.667s][info][gc,heap      ] GC(2) Survivor regions: 2->3(3)
      |[33.667s][info][gc,heap      ] GC(2) Old regions: 12->26
      |[33.667s][info][gc,heap      ] GC(2) Humongous regions: 0->0
      |[33.667s][info][gc,metaspace ] GC(2) Metaspace: 10007K->10007K(1058816K)
      |[33.667s][info][gc           ] GC(2) Pause Young (G1 Evacuation Pause) 58M->57M(502M) 18.686ms
      |[33.667s][info][gc,cpu       ] GC(2) User=0.05s Sys=0.06s Real=0.02s
      |[43.567s][info][gc,start     ] GC(3) Pause Young (G1 Evacuation Pause)
      |[43.567s][info][gc,task      ] GC(3) Using 8 workers of 8 for evacuation
      |[43.580s][info][gc,phases    ] GC(3)   Pre Evacuate Collection Set: 0.0ms
      |[43.580s][info][gc,phases    ] GC(3)   Evacuate Collection Set: 12.9ms
      |[43.580s][info][gc,phases    ] GC(3)   Post Evacuate Collection Set: 0.2ms
      |[43.580s][info][gc,phases    ] GC(3)   Other: 0.2ms
      |[43.580s][info][gc,heap      ] GC(3) Eden regions: 18->0(22)
      |[43.580s][info][gc,heap      ] GC(3) Survivor regions: 3->3(3)
      |[43.580s][info][gc,heap      ] GC(3) Old regions: 26->44
      |[43.580s][info][gc,heap      ] GC(3) Humongous regions: 0->0
      |[43.580s][info][gc,metaspace ] GC(3) Metaspace: 10007K->10007K(1058816K)
      |[43.580s][info][gc           ] GC(3) Pause Young (G1 Evacuation Pause) 93M->92M(502M) 13.496ms
      |[43.580s][info][gc,cpu       ] GC(3) User=0.03s Sys=0.07s Real=0.01s
      |
    """.stripMargin

}
