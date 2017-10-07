package info.batey.flows

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.TestKit
import org.scalatest.{Matchers, WordSpec, WordSpecLike}

class PauseFlowTest extends TestKit(ActorSystem("PauseFlowTest")) with WordSpecLike with Matchers {

  "a pause flow" must {
    "do something good"  in {
      val underTest: Flow[String, Int, NotUsed] = PauseFlow.flow

      TestSource.probe[String]
        .via(underTest)
    }
  }
}
