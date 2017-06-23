/* LogEventsWithAkkaSpec.scala
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

import akka.event.{Logging, LoggingAdapter}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
 * Test suite for [[atmos.monitor.LogEventsWithAkka]].
 */
class LogEventsWithAkkaSpec extends FlatSpec with Matchers with MockFactory {

  val thrown = new RuntimeException

  "LogEventsWithAkka" should "forward log entries to an Akka logging adapter" in {
    val fixture = new LoggerFixture
    val monitor = LogEventsWithAkka(fixture.mock)
    for {
      level <- Seq(Logging.ErrorLevel, Logging.WarningLevel, Logging.InfoLevel, Logging.DebugLevel)
      enabled <- Seq(true, false)
      t <- Seq(Some(thrown), None)
    } {
      fixture.isEnabled.expects(level).returns(enabled).once
      monitor.isLoggable(level) shouldBe enabled
      if (level == Logging.ErrorLevel && t.isDefined) fixture.error.expects(thrown, "MSG").once
      else fixture.log.expects(level, "MSG").once
      monitor.log(level, "MSG", t)
    }
  }

  class LoggerFixture {
    self =>
    val isEnabled = mockFunction[Logging.LogLevel, Boolean]
    val error = mockFunction[Throwable, String, Unit]
    val log = mockFunction[Logging.LogLevel, String, Unit]
    val mock = new LoggingAdapter {
      def isErrorEnabled = self.isEnabled(Logging.ErrorLevel)

      def isWarningEnabled = self.isEnabled(Logging.WarningLevel)

      def isInfoEnabled = self.isEnabled(Logging.InfoLevel)

      def isDebugEnabled = self.isEnabled(Logging.DebugLevel)

      def notifyError(message: String) = ???

      def notifyError(cause: Throwable, message: String) = ???

      def notifyWarning(message: String) = ???

      def notifyInfo(message: String) = ???

      def notifyDebug(message: String) = ???

      override def error(thrown: Throwable, message: String) = self.error(thrown, message)

      override def log(level: Logging.LogLevel, message: String) = self.log(level, message)
    }
  }

}