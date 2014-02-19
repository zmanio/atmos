/* EventMonitorSpec.scala
 * 
 * Copyright (c) 2013 bizo.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Portions of this code are derived from https://github.com/aboisvert/pixii
 * and https://github.com/lpryor/squishy.
 */
package atmos.retries

import java.io.{ ByteArrayOutputStream, PrintStream, PrintWriter }
import scala.concurrent.duration._
import org.scalatest._

/**
 * Test suite for [[atmos.retries.EventMonitor]].
 */
class EventMonitorSpec extends FlatSpec with Matchers {

  import EventMonitor._

  val thrown = new RuntimeException

  "EventMonitor.PrintEventsWithStream" should "format and print information about retry events to a stream" in {
    import PrintEvents.PrintAction._
    val target = new PrintEventsTarget
    for (action <- Seq(PrintNothing, PrintMessage, PrintMessageAndStackTrace)) {
      val monitor = PrintEventsWithStream(new PrintStream(target, true), action, action, action)
      monitor.retrying(None, thrown, 1, 1.second, true)
      target.complete() shouldBe PrintNothing
      monitor.retrying(Some("test"), thrown, 2, 1.second, false)
      target.complete() shouldBe action
      monitor.interrupted(None, thrown, 3)
      target.complete() shouldBe action
      monitor.interrupted(Some("test"), thrown, 4)
      target.complete() shouldBe action
      monitor.aborted(None, thrown, 5)
      target.complete() shouldBe action
      monitor.aborted(Some("test"), thrown, 6)
      target.complete() shouldBe action
    }
  }

  "EventMonitor.PrintEventsWithWriter" should "format and print information about retry events to a writer" in {
    import PrintEvents.PrintAction._
    val target = new PrintEventsTarget
    for (action <- Seq(PrintNothing, PrintMessage, PrintMessageAndStackTrace)) {
      val monitor = PrintEventsWithWriter(new PrintWriter(target, true), action, action, action)
      monitor.retrying(None, thrown, 1, 1.second, true)
      target.complete() shouldBe PrintNothing
      monitor.retrying(Some("test"), thrown, 2, 1.second, false)
      target.complete() shouldBe action
      monitor.interrupted(None, thrown, 3)
      target.complete() shouldBe action
      monitor.interrupted(Some("test"), thrown, 4)
      target.complete() shouldBe action
      monitor.aborted(None, thrown, 5)
      target.complete() shouldBe action
      monitor.aborted(Some("test"), thrown, 6)
      target.complete() shouldBe action
    }
  }

  /**
   * A subclass of `ByteArrayOutputStream` that can infer a `PrintAction` from the text it is given.
   */
  class PrintEventsTarget extends ByteArrayOutputStream {

    /** Infers a `PrintAction` from the current value of the buffer and subsequently clears the buffer. */
    def complete(): PrintEvents.PrintAction = {
      val txt = toString.trim
      reset()
      import PrintEvents.PrintAction._
      if (txt.isEmpty) PrintNothing
      else if (txt.split("[\r\n]+").size <= 2) PrintMessage
      else PrintMessageAndStackTrace
    }

  }

}