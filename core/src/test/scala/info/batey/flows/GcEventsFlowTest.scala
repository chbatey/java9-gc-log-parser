package info.batey.flows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import info.batey.actors.GcStateActor._
import info.batey.flows.CollectByGcEvent.CollectedGcLines
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

class GcEventsFlowTest extends TestKit(ActorSystem("PauseFlowTest")) with WordSpecLike with Matchers {

  val waitTime = 50.milliseconds
  implicit val materialiser = ActorMaterializer()

  "a pause flow" must {
    "do something good" in {
      val underTest = GcEventsFlow.flow

      val (pub, sub) = TestSource.probe[CollectedGcLines]
        .via(underTest)
        .toMat(TestSink.probe[GcEvent])(Keep.both)
        .run()
    }
  }
}

