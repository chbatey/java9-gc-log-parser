package info.batey.flows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.stream.testkit.scaladsl._
import info.batey.GCLogFileModel._
import info.batey.TestKitWithCleanup
import info.batey.flows.CollectByGcEvent.{CollectedGcLines, LinesForPause, SingleLineInfo}

import scala.concurrent.duration._

class CollectByGcEventTest extends TestKitWithCleanup(ActorSystem("CollectByGcEventTest")) {

  implicit val materialiser = ActorMaterializer()
  val waitTime = 100.milliseconds

  "gc log line collector" must {
    "emit lines without an eventId right away" in {
      val (pub, sub) = probe()
      val isolatedLine = G1GcLine(Metadata(1, None), UsingG1)
      pub.sendNext(isolatedLine)
      sub.requestNext() should equal(SingleLineInfo(isolatedLine))
    }

    "should collect all events with the same eventId" in {
      val (pub, sub) = probe()
      val start = G1GcLine(Metadata(1, Some(1)), PauseStart(Young, None))
      val another = G1GcLine(Metadata(2, Some(1)), NrRegions(Eden, 5, 4))
      val end = G1GcLine(Metadata(3, Some(1)), PauseEnd(Young, CollectionStats(1,2,3,1.second), None))

      pub.sendNext(start)
      sub.request(1)
      sub.expectNoMessage(waitTime)
      pub.sendNext(another)
      sub.expectNoMessage(waitTime)
      pub.sendNext(end)
      sub.expectNext() should equal(LinesForPause(List(start, another, end)))
    }
  }

  def probe(): (TestPublisher.Probe[Line], TestSubscriber.Probe[CollectedGcLines]) = {

    TestSource.probe[Line]
      .via(CollectByGcEvent.CollectByGcEvent)
      .toMat(TestSink.probe[CollectedGcLines])(Keep.both)
      .run()

  }
}
