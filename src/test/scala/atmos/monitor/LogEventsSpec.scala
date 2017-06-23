/* LogEventsSpec.scala
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
 * Test suite for [[atmos.monitor.LogEvents]].
 */
class LogEventsSpec extends FlatSpec with Matchers with MockFactory {

  import LogAction._

  val result = "result"
  val thrown = new RuntimeException

  "LogEvents" should "submit relevant log entries to the underlying target" in {
    for {
      (action, failAction) <- Seq(
        LogNothing -> LogAt(Lvl.Error),
        LogAt(Lvl.Error) -> LogAt(Lvl.Warn),
        LogAt(Lvl.Warn) -> LogNothing)
      selector <- Seq(
        EventClassifier.empty[LogAction[Lvl]],
        EventClassifier { case Failure(t) if t == thrown => failAction })
      fixture = new LogEventsFixture(action, selector)
      enabled <- Seq(true, false)
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 10L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        if (!silent) fixture.expectsOnce(enabled, outcome)
        fixture.mock.retrying(name, outcome, attempt, backoff, silent)
      }
      fixture.expectsOnce(enabled, outcome)
      fixture.mock.interrupted(name, outcome, attempt)
      fixture.expectsOnce(enabled, outcome)
      fixture.mock.aborted(name, outcome, attempt)
    }
  }

  class LogEventsFixture(action: LogAction[Lvl], selector: EventClassifier[LogAction[Lvl]]) {
    self =>
    val isLoggable = mockFunction[Lvl, Boolean]
    val log = mockFunction[Lvl, String, Option[Throwable], Unit]
    val mock = new LogEvents {
      type LevelType = Lvl
      val retryingAction = action
      val interruptedAction = action
      val abortedAction = action
      val retryingActionSelector = selector
      val interruptedActionSelector = selector
      val abortedActionSelector = selector

      def isLoggable(level: Lvl) = self.isLoggable(level)

      def log(level: Lvl, message: String, thrown: Option[Throwable]) = self.log(level, message, thrown)
    }

    def expectsOnce(enabled: Boolean, outcome: Try[String]) =
      selector.applyOrElse(outcome, (_: Try[Any]) => action) match {
        case LogAt(lvl) =>
          isLoggable.expects(lvl).returning(enabled).once
          if (enabled) log.expects(lvl, *, if (outcome isFailure) Some(thrown) else None).once
        case LogNothing =>
      }
  }

  sealed trait Lvl

  object Lvl {
    object Error extends Lvl
    object Warn extends Lvl
  }

}