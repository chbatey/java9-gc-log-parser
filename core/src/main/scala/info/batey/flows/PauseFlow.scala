package info.batey.flows

import akka.NotUsed
import akka.stream.scaladsl._

object PauseFlow {

  val flow: Flow[String, Int, NotUsed] =
    Flow[String].statefulMapConcat(() => (s: String) => {
      List.empty[Int]
    })

}
