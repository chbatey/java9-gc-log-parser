package info.batey

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Matchers, Suite, WordSpecLike}

abstract class TestKitWithCleanup(as: ActorSystem) extends TestKit(as)
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  override protected def afterAll(): Unit = {
    as.terminate()
  }
}
