package info.batey.flows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import info.batey.GCLogFileModel._
import info.batey.GcStateModel._
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

/*
 * Contains the logic for bring together lines for a pause.
 */
class GcStateFlowTest extends TestKit(ActorSystem("PauseFlowTest")) with WordSpecLike with Matchers {

  val waitTime = 50.milliseconds
  implicit val materialiser = ActorMaterializer()

  "gc state flow" must {
    "aggregate young gens" in {
      val (pub, sub) = probe()

      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(1, 2, 3, 4)
      pub.sendNext(Pause(1, Young, 1.second, heapSizes, genSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        0,
        youngGcs = 1,
        0, 0, 0, 0, HeapSize(heapSizes.after, heapSizes.total), genSizes))

      val newHeapSizes = HeapSizes(100, 50, 200)
      val newGenSizes = GenerationSizes(11, 12, 13, 14)
      pub.sendNext(Pause(2, Young, 2.second, newHeapSizes, newGenSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 2,
        0,
        youngGcs = 2,
        0, 0, 0, 0, HeapSize(newHeapSizes.after, newHeapSizes.total), newGenSizes))
    }

    "aggregate initialMarks pauses" in {
      val (pub, sub) = probe()
      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(1, 2, 3, 4)
      pub.sendNext(Pause(1, InitialMark, 1.second, heapSizes, genSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        0,
        youngGcs = 0,
        initialMarks = 1,
        0, 0, 0, HeapSize(heapSizes.after, heapSizes.total), genSizes))
    }

    "aggregate full pauses" in {
      val (pub, sub) = probe()
      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(1, 2, 3, 4)
      pub.sendNext(Pause(1, Full, 1.second, heapSizes, genSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        fullGcs = 1,
        youngGcs = 0,
        initialMarks = 0,
        0, 0, 0, HeapSize(heapSizes.after, heapSizes.total), genSizes))
    }

    "aggregate mixed pauses" in {
      val (pub, sub) = probe()
      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(1, 2, 3, 4)
      pub.sendNext(Pause(1, Mixed, 1.second, heapSizes, genSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        fullGcs = 0,
        youngGcs = 0,
        initialMarks = 0,
        0,
        mixed = 1, 0, HeapSize(heapSizes.after, heapSizes.total), genSizes))
    }

    "aggregate cleanup pauses" in {
      val (pub, sub) = probe()
      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(1, 2, 3, 4)
      pub.sendNext(Pause(1, Cleanup, 1.second, heapSizes, genSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        fullGcs = 0,
        youngGcs = 0,
        initialMarks = 0,
        remarks = 0,
        mixed = 0,
        cleanups = 1,
        HeapSize(heapSizes.after, heapSizes.total), genSizes))
    }

    "aggregate remarks" in {
      val (pub, sub) = probe()
      val heapSizes = HeapSizes(10, 5, 20)
      val genSizes = GenerationSizes(0, 0, 0, 0)
      pub.sendNext(RemarkPause(1, 1.second, heapSizes))
      sub.requestNext() should equal(GcState(
        timeOffset = 1,
        fullGcs = 0,
        youngGcs = 0,
        initialMarks = 0,
        remarks = 1,
        mixed = 0,
        cleanups = 0,
        HeapSize(heapSizes.after, heapSizes.total), genSizes))
    }

    def probe(): (TestPublisher.Probe[GcEvent], TestSubscriber.Probe[GcState]) = {
      TestSource.probe[GcEvent]
        .via(GcStateFlow.GcStateFlow)
        .toMat(TestSink.probe[GcState])(Keep.both)
        .run
    }
  }
}

