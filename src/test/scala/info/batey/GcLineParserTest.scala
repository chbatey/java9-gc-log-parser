package info.batey

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, FunSuite, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class GcLineParserTest extends FunSpec with Matchers {

  import GcLineParser._
  import TimeOffset._

  val lines = Table(
    ("gc_line", "outcome"),

    ("[0.010s][info][gc] Using G1",
      GcInfo(Metadata(10L, Info), "Using G1")),

    ("[39.708s][info][gc] GC(0) Pause Young (G1 Evacuation Pause) 24M->8M(256M) 6.545ms",
      YoungPause(Metadata(39708L, Info), CollectionStats(24, 8, 256, 6.545 milliseconds), Evacuation)),

    ("[555.879s][info][gc] GC(8) Pause Initial Mark (G1 Evacuation Pause) 185M->159M(256M) 1.354ms",
      InitialMark(Metadata(555879L, Info), CollectionStats(185, 159, 256, 1.354 milliseconds), Evacuation)),

    ("[555.879s][info][gc] GC(9) Concurrent Cycle",
      ConcurrentCycle(Metadata(555879L, Info))),

    ("[613.102s][info][gc] GC(15) Pause Remark 149M->149M(256M) 1.381ms",
      Remark(Metadata(613102L, Info), CollectionStats(149, 149, 256, 1.381 milliseconds))),

    ("[513.382s][info][gc] GC(9) Pause Cleanup 202M->39M(312M) 0.369ms",
      Cleanup(Metadata(513382L, Info), CollectionStats(202, 39, 312, 0.369 milliseconds))),

    ("[711.229s][info][gc] GC(23) Pause Mixed (G1 Evacuation Pause) 159M->151M(312M) 4.898ms",
      MixedPause(Metadata(711229L, Info), CollectionStats(159, 151, 312, 4.898 milliseconds), Evacuation)),

    ("[2646.462s][info][gc] GC(484) Pause Initial Mark (G1 Humongous Allocation) 813M->817M(876M) 8.092ms",
      InitialMark(Metadata(2646462L), CollectionStats(813, 817, 876, 8.092 milliseconds), HumongousAllocation))

  )

  forAll(lines) { (line: String, outcome: G1GcEvent) => {
    parse(gcLine, line).get should equal(outcome)
  }
  }
}

