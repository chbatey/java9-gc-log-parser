package info.batey

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps
import GcLineParser._
import GCLogFileModel._
import TimeOffset._
import org.scalatest.prop.TableDrivenPropertyChecks

class GcLineParserTest extends FunSpec with Matchers {

  val lines = Table(
    ("gc_line", "outcome"),

    ("[0.010s][info][gc] Using G1",
      G1GcEvent(Metadata(10L, Info), UsingG1)),

    ("[39.708s][info][gc] GC(0) Pause Young (G1 Evacuation Pause) 24M->8M(256M) 6.545ms",
      G1GcEvent(Metadata(39708L), Pause(Young, CollectionStats(24, 8, 256, 6.545 milliseconds), Some(Evacuation)))),

    ("[555.879s][info][gc] GC(8) Pause Initial Mark (G1 Evacuation Pause) 185M->159M(256M) 1.354ms",
      G1GcEvent(Metadata(555879L), Pause(InitialMark, CollectionStats(185, 159, 256, 1.354 milliseconds), Some(Evacuation)))),

    ("[555.879s][info][gc] GC(9) Concurrent Cycle",
      G1GcEvent(Metadata(555879L), ConcurrentCycle)),

    ("[613.102s][info][gc] GC(15) Pause Remark 149M->149M(256M) 1.381ms",
      G1GcEvent(Metadata(613102L, Info), Pause(Remark, CollectionStats(149, 149, 256, 1.381 milliseconds), None))),

    ("[513.382s][info][gc] GC(9) Pause Cleanup 202M->39M(312M) 0.369ms",
      G1GcEvent(Metadata(513382L, Info), Pause(Cleanup, CollectionStats(202, 39, 312, 0.369 milliseconds), None))),

    ("[711.229s][info][gc] GC(23) Pause Mixed (G1 Evacuation Pause) 159M->151M(312M) 4.898ms",
      G1GcEvent(Metadata(711229L, Info), Pause(Mixed, CollectionStats(159, 151, 312, 4.898 milliseconds), Some(Evacuation)))),

    ("[2646.462s][info][gc] GC(484) Pause Initial Mark (G1 Humongous Allocation) 813M->817M(876M) 8.092ms",
      G1GcEvent(Metadata(2646462L), Pause(InitialMark, CollectionStats(813, 817, 876, 8.092 milliseconds), Some(HumongousAllocation)))),

    ("[83.923s][info][gc] GC(22) Pause Full (Allocation Failure) 256M->230M(256M) 44.683ms",
      G1GcEvent(Metadata(83923L), Pause(Full, CollectionStats(256, 230, 256, 44.683 milliseconds), Some(AllocationFailure)))),

    ("[83.997s][info][gc] GC(20) To-space exhausted",
      G1GcEvent(Metadata(83997L), ToSpaceExhausted))
  )

  val heapLines = Table(
    ("gc_line", "outcome"),
    ("[0.007s][info][gc,heap] Heap region size: 1M",
      G1GcEvent(Metadata(7, Info, Set(Gc, Heap)), RegionSize(1)))

  )

  val phasesLines = Table(
    ("line", "outcome"),
    ("[4.403s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms",
      G1GcEvent(Metadata(4403, Info, Set(Gc, Phases)), Phase("Pre Evacuate Collection Set", 0 milliseconds)))
  )

  forAll(phasesLines) { (line: String, outcome: G1GcEvent) => {
    parse(gcLine, line).get should equal(outcome)
  }
  }
}

class PhaseParserTest extends TableDrivenPropertyChecks with Matchers {
  val phaseExamples = Table(
    ("text", "outcome"),
    ("   Pre Evacuate Collection Set: 0.0ms", Phase("Pre Evacuate Collection Set", 0 milliseconds))
  )

  forAll(phaseExamples) { (line: String, outcome: Phase) => {
    parse(phases, line).get should equal(outcome)
  }
  }
}

class ReasonParserTest extends FunSpec with Matchers {
  val reasons = Table(
    ("text", "outcome"),
    ("(G1 Evacuation Pause)", Evacuation)
  )

  forAll(reasons) { (line: String, outcome: Reason) => {
    parse(reason, line).get should equal(outcome)
  }
  }
}

class MetadataParserTest extends FunSpec with Matchers {
  val metadataLines = Table(
    ("text", "outcome"),
    ("[0.007s][info][gc,heap]", Metadata(7, Info, Set(Gc, Heap))),
    ("[0.007s][info][gc,phases    ]", Metadata(7, Info, Set(Gc, Phases)))
  )

  forAll(metadataLines) { (line: String, outcome: Metadata) => {
    parse(header, line).get should equal(outcome)
  }
  }
}

