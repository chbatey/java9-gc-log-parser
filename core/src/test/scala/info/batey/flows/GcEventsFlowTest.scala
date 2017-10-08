package info.batey.flows

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import info.batey.GCLogFileModel._
import info.batey.actors.GcStateActor._
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

/*
 * Contains the logic for bring together lines for a pause.
 */
class GcEventsFlowTest extends TestKit(ActorSystem("PauseFlowTest")) with WordSpecLike with Matchers {

  val waitTime = 50.milliseconds
  implicit val materialiser = ActorMaterializer()

  "gc events flow" must {
    "build pause info" in {

    }

  }

  private def line(desc: LineDesc): G1GcLine = {
    G1GcLine(Metadata(1, Some(1)), desc)
  }
}

