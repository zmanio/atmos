/* PrintEventsSpec.scala
 * 
 * Copyright (c) 2013-2014 linkedin.com
 * Copyright (c) 2013-2015 zman.io
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

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Test suite for [[atmos.monitor.PrintEvents]].
 */
class PrintEventsSpec extends FlatSpec with Matchers with MockFactory {

  import PrintAction._

  val result = "result"
  val thrown = new RuntimeException

  "PrintEvents" should "forward relevant event messages to the underlying target" in {
    for {
      (action, failAction) <- Seq(
        PrintNothing -> PrintMessage,
        PrintMessage -> PrintMessageAndStackTrace,
        PrintMessageAndStackTrace -> PrintNothing)
      selector <- Seq(
        EventClassifier.empty[PrintAction],
        EventClassifier { case Failure(t) if t == thrown => failAction })
      fixture = new PrintEventsFixture(action, selector)
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 10L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        if (!silent) fixture.expectsOnce(outcome)
        fixture.mock.retrying(name, outcome, attempt, backoff, silent)
      }
      fixture.expectsOnce(outcome)
      fixture.mock.interrupted(name, outcome, attempt)
      fixture.expectsOnce(outcome)
      fixture.mock.aborted(name, outcome, attempt)
    }
  }

  class PrintEventsFixture(action: PrintAction, selector: EventClassifier[PrintAction]) {
    self =>
    val printMessage = mockFunction[String, Unit]
    val printMessageAndStackTrace = mockFunction[String, Throwable, Unit]
    val mock = new PrintEvents {
      val retryingAction = action
      val interruptedAction = action
      val abortedAction = action
      val retryingActionSelector = selector
      val interruptedActionSelector = selector
      val abortedActionSelector = selector

      def printMessage(message: String) = self.printMessage(message)

      def printMessageAndStackTrace(message: String, thrown: Throwable) =
        self.printMessageAndStackTrace(message, thrown)
    }

    def expectsOnce(outcome: Try[String]) = selector.applyOrElse(outcome, (_: Try[Any]) => action) match {
      case PrintMessageAndStackTrace if outcome isFailure =>
        printMessageAndStackTrace.expects(*, thrown).once
      case PrintNothing =>
      case _ =>
        printMessage.expects(*).once
    }
  }

}