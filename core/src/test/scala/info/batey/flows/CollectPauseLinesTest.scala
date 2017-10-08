package info.batey.flows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.testkit.scaladsl._
import info.batey.GCLogFileModel._
import info.batey.TestKitWithCleanup
import info.batey.GcStateModel._

import scala.concurrent.duration._

class CollectPauseLinesTest extends TestKitWithCleanup(ActorSystem("CollectByGcEventTest")) {

  implicit val materialiser = ActorMaterializer()
  val waitTime = 100.milliseconds

  "gc log line collector" must {
    //todo add these to GcEvent
    "emit lines without an eventId right away" in {
      val (pub, sub) = probe()
      val isolatedLine = G1GcLine(Metadata(1, None), UsingG1)
      pub.sendNext(isolatedLine)
      sub.requestNext() should equal(NotInteresting())
    }

    "collect all events for a pause" in {
      val (pub, sub) = probe()
      val pauseType = Young
      val cs = CollectionStats(1, 2, 3, 1.second)
      val (nrEden, nrSurvivor, nrOld, nrHum) = (0, 2, 3, 4)
      val start = G1GcLine(Metadata(1, Some(1)), PauseStart(pauseType, None))
      val edenRegion = G1GcLine(Metadata(2, Some(1)), NrRegions(Eden, 12, 0))
      val survivorRegion = G1GcLine(Metadata(2, Some(1)), NrRegions(Survivor, 0, nrSurvivor))
      val oldRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Old, nrOld, nrOld))
      val humongousRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Humongous, nrHum, nrHum))
      val end = G1GcLine(Metadata(3, Some(1)), PauseEnd(pauseType, cs, None))

      pub.sendNext(start)
      sub.request(1)
      sub.expectNoMessage(waitTime)
      pub.sendNext(edenRegion)
      sub.expectNoMessage(waitTime)
      pub.sendNext(survivorRegion)
      sub.expectNoMessage(waitTime)
      pub.sendNext(oldRegions)
      sub.expectNoMessage(waitTime)
      pub.sendNext(humongousRegions)
      sub.expectNoMessage(waitTime)
      pub.sendNext(end)
      sub.expectNext() should equal(DetailedPause(
        end.metadata.offset,
        pauseType,
        cs.duration,
        HeapSizes(cs.before, cs.after, cs.total),
        GenerationSizes(nrEden, nrSurvivor, nrOld, nrHum)
      ))
    }

    " process multiple events" in {
      val (pub, sub) = probe()
      val pauseType = Young
      val cs = CollectionStats(1, 2, 3, 1.second)
      val (nrEden, nrSurvivor, nrOld, nrHum) = (0, 2, 3, 4)
      val ps = PauseStart(pauseType, None)
      val start = G1GcLine(Metadata(1, Some(1)), ps)
      val edenRegion = G1GcLine(Metadata(2, Some(1)), NrRegions(Eden, 12, 0))
      val nrRegionsSurvivor = NrRegions(Survivor, 0, nrSurvivor)
      val survivorRegion = G1GcLine(Metadata(2, Some(1)), nrRegionsSurvivor)
      val oldRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Old, nrOld, nrOld))
      val humongousRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Humongous, nrHum, nrHum))
      val pe = PauseEnd(pauseType, cs, None)
      val end = G1GcLine(Metadata(3, Some(1)), pe)

      pub.sendNext(start)
      pub.sendNext(edenRegion)
      pub.sendNext(survivorRegion)
      pub.sendNext(oldRegions)
      pub.sendNext(humongousRegions)
      pub.sendNext(end)

      sub.requestNext() should equal(DetailedPause(
        end.metadata.offset,
        pauseType,
        cs.duration,
        HeapSizes(cs.before, cs.after, cs.total),
        GenerationSizes(nrEden, nrSurvivor, nrOld, nrHum)
      ))

      pub.sendNext(start.copy(event = ps.copy(which = Full)))
      pub.sendNext(edenRegion)
      val nrSurNew = 100
      pub.sendNext(survivorRegion.copy(event = nrRegionsSurvivor.copy(after = nrSurNew)))
      pub.sendNext(oldRegions)
      pub.sendNext(humongousRegions)
      pub.sendNext(end.copy(event = pe.copy(which = Full)))

      sub.requestNext() should equal(DetailedPause(
        end.metadata.offset,
        Full,
        cs.duration,
        HeapSizes(cs.before, cs.after, cs.total),
        GenerationSizes(nrEden, nrSurNew, nrOld, nrHum)
      ))
    }

    "error stream if not all lines for pause" in {
      val (pub, sub) = probe()
      val pauseType = Young
      val cs = CollectionStats(1, 2, 3, 1.second)
      val start = G1GcLine(Metadata(1, Some(1)), PauseStart(pauseType, None))
      val end = G1GcLine(Metadata(3, Some(1)), PauseEnd(pauseType, cs, None))

      pub.sendNext(start)
      pub.sendNext(end)
      sub.request(1)
      sub.expectError().getMessage should equal("Received PauseEnd without receiving region information. Either a bug or invalid GC log. Regions: {}. EventId: 1")
    }

    "should clear stream between events" in {
      val (pub, sub) = probe()
      val pauseType = Young
      val cs = CollectionStats(1, 2, 3, 1.second)
      val (nrEden, nrSurvivor, nrOld, nrHum) = (0, 2, 3, 4)
      val start = G1GcLine(Metadata(1, Some(1)), PauseStart(pauseType, None))
      val edenRegion = G1GcLine(Metadata(2, Some(1)), NrRegions(Eden, 12, nrEden))
      val nrRegionsSurvivor = NrRegions(Survivor, 0, nrSurvivor)
      val survivorRegion = G1GcLine(Metadata(2, Some(1)), nrRegionsSurvivor)
      val oldRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Old, nrOld, nrOld))
      val humongousRegions = G1GcLine(Metadata(2, Some(1)), NrRegions(Humongous, nrHum, nrHum))
      val end = G1GcLine(Metadata(3, Some(1)), PauseEnd(pauseType, cs, None))

      pub.sendNext(start)
      pub.sendNext(edenRegion)
      pub.sendNext(survivorRegion)
      pub.sendNext(oldRegions)
      pub.sendNext(humongousRegions)
      pub.sendNext(end)

      sub.requestNext()

      pub.sendNext(end)
      sub.request(1)
      sub.expectError().getLocalizedMessage should equal("Received PauseEnd before PauseStart. Either a bug or invalid GC log. EventId: 1")
    }

    "handle a remark" in {
      val startRemark = G1GcLine(Metadata(TimeOffset(101230), Some(7), Info, Set(Gc, Start)), PauseStart(Remark, None))
      val stats = CollectionStats(436, 436, 644, 5483.microseconds)
      val endRemark = G1GcLine(Metadata(TimeOffset(101235), Some(7), Info, Set(Gc)), PauseEnd(Remark, stats, None))
      val (pub, sub) = probe()

      pub.sendNext(startRemark)
      sub.request(1)
      sub.expectNoMessage(waitTime)
      pub.sendNext(endRemark)
      sub.expectNext() should equal(RemarkPause(
        endRemark.metadata.offset,
        stats.duration,
        HeapSizes.fromStats(stats)
      ))
    }
  }

  def probe(): (TestPublisher.Probe[Line], TestSubscriber.Probe[GcEvent]) = {

    TestSource.probe[Line]
      .via(CollectPauseLines.CollectByGcEvent)
      .toMat(TestSink.probe[GcEvent])(Keep.both)
      .run()
  }
}
