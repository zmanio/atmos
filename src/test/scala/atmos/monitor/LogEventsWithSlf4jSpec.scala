/* LogEventsWithSlf4jSpec.scala
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
import org.slf4j.Logger

/**
 * Test suite for [[atmos.monitor.LogEventsWithSlf4j]].
 */
class LogEventsWithSlf4jSpec extends FlatSpec with Matchers with MockFactory {

  import LogEventsWithSlf4j.Slf4jLevel

  val thrown = new RuntimeException

  "LogEventsWithSlf4j" should "forward log entries to a Slf4j logger" in {
    for {
      level <- Seq(Slf4jLevel.Error, Slf4jLevel.Warn, Slf4jLevel.Info, Slf4jLevel.Debug, Slf4jLevel.Trace)
      fixture = new LoggerFixture(level)
      monitor = LogEventsWithSlf4j(fixture.logger)
      enabled <- Seq(true, false)
      t <- Seq(Some(thrown), None)
    } {
      fixture.expectIsEnabledOnce(enabled)
      monitor.isLoggable(level) shouldBe enabled
      fixture.expectLogOnce("MSG", t)
      monitor.log(level, "MSG", t)
    }
  }

  class LoggerFixture(level: Slf4jLevel) {
    val logger = mock[MockLogger]

    def expectIsEnabledOnce(enabled: Boolean) = level match {
      case Slf4jLevel.Error => (logger.isErrorEnabled _).expects().returns(enabled).once
      case Slf4jLevel.Warn => (logger.isWarnEnabled _).expects().returns(enabled).once
      case Slf4jLevel.Info => (logger.isInfoEnabled _).expects().returns(enabled).once
      case Slf4jLevel.Debug => (logger.isDebugEnabled _).expects().returns(enabled).once
      case Slf4jLevel.Trace => (logger.isTraceEnabled _).expects().returns(enabled).once
    }

    def expectLogOnce(message: String, thrown: Option[Throwable]) = thrown match {
      case Some(t) => level match {
        case Slf4jLevel.Error => (logger.error(_: String, _: Throwable)).expects(message, t).once
        case Slf4jLevel.Warn => (logger.warn(_: String, _: Throwable)).expects(message, t).once
        case Slf4jLevel.Info => (logger.info(_: String, _: Throwable)).expects(message, t).once
        case Slf4jLevel.Debug => (logger.debug(_: String, _: Throwable)).expects(message, t).once
        case Slf4jLevel.Trace => (logger.trace(_: String, _: Throwable)).expects(message, t).once
      }
      case None => level match {
        case Slf4jLevel.Error => (logger.error(_: String)).expects(message).once
        case Slf4jLevel.Warn => (logger.warn(_: String)).expects(message).once
        case Slf4jLevel.Info => (logger.info(_: String)).expects(message).once
        case Slf4jLevel.Debug => (logger.debug(_: String)).expects(message).once
        case Slf4jLevel.Trace => (logger.trace(_: String)).expects(message).once
      }
    }
  }

  // A trait that presents a narrow view of Slf4j loggers to help ScalaMock resolve the correct overloaded method.
  trait MockLogger extends Logger {
    def trace(s: String): Unit

    def debug(s: String): Unit

    def info(s: String): Unit

    def warn(s: String): Unit

    def error(s: String): Unit

    def trace(s: String, t: Throwable): Unit

    def debug(s: String, t: Throwable): Unit

    def info(s: String, t: Throwable): Unit

    def warn(s: String, t: Throwable): Unit

    def error(s: String, t: Throwable): Unit
  }

}