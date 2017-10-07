package info.batey

import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

class DurationParsingTest extends FunSuite with Matchers {

  import org.scalatest.prop.TableDrivenPropertyChecks._
  import GcLineParser._

  val durations = Table(
    ("duration", "value"),
    ("1.2ms", 1.2 milliseconds),
    ("0.010s", 0.01 seconds)
  )
  forAll(durations) { (str: String, dur: Duration) => {
    parse(offset, str).get should equal(dur)
  }
  }
}
