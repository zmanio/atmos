/* PrintEventsSpec.scala
 * 
 * Copyright (c) 2013-2014 bizo.com
 * Copyright (c) 2013-2014 zman.io
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
 */
package atmos.monitor

import scala.concurrent.duration._
import org.scalatest._
import org.scalamock.scalatest.MockFactory

/**
 * Test suite for [[atmos.monitor.PrintEvents]].
 */
class PrintEventsSpec extends FlatSpec with Matchers with MockFactory {
  
  import PrintAction._

  val thrown = new RuntimeException

  "PrintEvents" should "forward relevant event messages to the underlying target" in {
    for {
      action <- Seq(PrintNothing, PrintMessage, PrintMessageAndStackTrace)
      fixture = new PrintEventsFixture(action)
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        if (!silent) fixture.expectsOnce()
        fixture.mock.retrying(name, thrown, attempt, backoff, silent)
      }
      fixture.expectsOnce()
      fixture.mock.interrupted(name, thrown, attempt)
      fixture.expectsOnce()
      fixture.mock.aborted(name, thrown, attempt)
    }
  }

  class PrintEventsFixture(action: PrintAction) { self =>
    val printMessage = mockFunction[String, Unit]
    val printMessageAndStackTrace = mockFunction[String, Throwable, Unit]
    val mock = new PrintEvents {
      val retryingAction = action
      val interruptedAction = action
      val abortedAction = action
      def printMessage(message: String) = self.printMessage(message)
      def printMessageAndStackTrace(message: String, thrown: Throwable) =
        self.printMessageAndStackTrace(message, thrown)
    }
    def expectsOnce() = action match {
      case PrintMessageAndStackTrace =>
        printMessageAndStackTrace.expects(*, thrown).once
      case PrintMessage =>
        printMessage.expects(*).once
      case PrintNothing =>
    }
  }

}