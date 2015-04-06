/* LogEventsSpec.scala
 * 
 * Copyright (c) 2013-2014 linkedin.com
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
 * Test suite for [[atmos.monitor.LogEvents]].
 */
class LogEventsSpec extends FlatSpec with Matchers with MockFactory {

  import LogAction._

  val thrown = new RuntimeException

  "LogEvents" should "submit relevant log entries to the underlying target" in {
    for {
      action <- Seq(LogNothing, LogAt(Lvl.Error), LogAt(Lvl.Warn))
      fixture = new LogEventsFixture(action)
      enabled <- Seq(true, false)
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        if (!silent) fixture.expectsOnce(enabled)
        fixture.mock.retrying(name, thrown, attempt, backoff, silent)
      }
      fixture.expectsOnce(enabled)
      fixture.mock.interrupted(name, thrown, attempt)
      fixture.expectsOnce(enabled)
      fixture.mock.aborted(name, thrown, attempt)
    }
  }

  class LogEventsFixture(action: LogAction[Lvl]) { self =>
    val isLoggable = mockFunction[Lvl, Boolean]
    val log = mockFunction[Lvl, String, Throwable, Unit]
    val mock = new LogEvents {
      type LevelType = Lvl
      val retryingAction = action
      val interruptedAction = action
      val abortedAction = action
      def isLoggable(level: Lvl) = self.isLoggable(level)
      def log(level: Lvl, message: String, thrown: Throwable) = self.log(level, message, thrown)
    }
    def expectsOnce(enabled: Boolean) = action match {
      case LogAt(lvl) =>
        isLoggable.expects(lvl).returning(enabled).once
        if (enabled) log.expects(lvl, *, thrown).once
      case LogNothing =>
    }
  }

  sealed trait Lvl

  object Lvl {
    object Error extends Lvl
    object Warn extends Lvl
  }

}