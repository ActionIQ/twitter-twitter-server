package com.twitter.server.util

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{StatsRegistry, StatEntry}
import com.twitter.util.Time
import org.scalatest.FunSuite

private[server] object MetricSourceTest {
  class Ctx {
    case class Entry(delta: Double, value: Double, metricType: String) extends StatEntry
    private[twitter] var underlying = Map[String, StatEntry]()
    val sr = new StatsRegistry {
      def apply() = underlying;
      override val latched: Boolean = false
    }
    val registry = { () =>
      Seq(sr)
    }
    val source = new MetricSource(registry, 1.second)
  }
}

class MetricSourceTest extends FunSuite {
  import MetricSourceTest._

  test("get") {
    Time.withCurrentTimeFrozen { tc =>
      val ctx = new Ctx
      import ctx._

      underlying = Map("clnt/foo/requests" -> Entry(0.0, 10.0, "counter"))
      assert(source.get("clnt/foo/requests") == None)

      tc.advance(1.second)
      assert(source.get("clnt/foo/requests").get.delta == 0.0)
      assert(source.get("clnt/foo/requests").get.value == 10.0)
    }
  }

  test("contains") {
    Time.withCurrentTimeFrozen { tc =>
      val ctx = new Ctx
      import ctx._

      underlying = Map("clnt/foo/requests" -> Entry(0.0, 0.0, "counter"))
      assert(source.contains("clnt/foo/requests") == false)

      tc.advance(1.second)
      assert(source.contains("clnt/foo/requests") == true)
    }
  }

  test("keySet") {
    Time.withCurrentTimeFrozen { tc =>
      val ctx = new Ctx
      import ctx._

      underlying = Map(
        "clnt/foo/requests" -> Entry(0.0, 0.0, "counter"),
        "clnt/foo/success" -> Entry(0.0, 0.0, "counter"))
      assert(source.keySet == Set.empty[String])
      tc.advance(1.second)
      assert(source.keySet == Set("clnt/foo/requests", "clnt/foo/success"))
    }
  }
}
