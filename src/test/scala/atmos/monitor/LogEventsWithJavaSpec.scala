/* LogEventsWithJavaSpec.scala
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

import java.util.logging.{Level, Logger}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
 * Test suite for [[atmos.monitor.LogEventsWithJava]].
 */
class LogEventsWithJavaSpec extends FlatSpec with Matchers with MockFactory {

  val thrown = new RuntimeException

  "LogEventsWithJava" should "forward log entries to a standard Java logger" in {
    val fixture = new LoggerFixture
    val monitor = LogEventsWithJava(fixture.mock)
    for {
      level <- Seq(Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG)
      enabled <- Seq(true, false)
      t <- Seq(Some(thrown), None)
    } {
      fixture.isLoggable.expects(level).returns(enabled).once
      monitor.isLoggable(level) shouldBe enabled
      t match {
        case Some(tt) => fixture.logThrown.expects(level, "MSG", tt).once
        case None => fixture.log.expects(level, "MSG").once
      }
      monitor.log(level, "MSG", t)
    }
  }

  class LoggerFixture {
    self =>
    val isLoggable = mockFunction[Level, Boolean]
    val log = mockFunction[Level, String, Unit]
    val logThrown = mockFunction[Level, String, Throwable, Unit]
    val mock = new Logger(null, null) {
      override def isLoggable(level: Level) = self.isLoggable(level)

      override def log(level: Level, message: String) = self.log(level, message)

      override def log(level: Level, message: String, thrown: Throwable) = self.logThrown(level, message, thrown)
    }
  }

}