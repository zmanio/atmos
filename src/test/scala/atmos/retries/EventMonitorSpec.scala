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
import java.util.logging.{ Logger, Level }
import scala.concurrent.duration._
import org.slf4j.{ Logger => Slf4jLogger }
import org.scalatest._
import org.scalamock.scalatest.MockFactory

/**
 * Test suite for [[atmos.retries.EventMonitor]].
 */
class EventMonitorSpec extends FlatSpec with Matchers with MockFactory {

  import EventMonitor._

  val thrown = new RuntimeException

  "EventMonitor.PrintEventsWithStream" should "format and print information about retry events to a stream" in {
    import PrintEvents.PrintAction._
    val target = new PrintMock
    for (action <- Seq(PrintNothing, PrintMessage, PrintMessageAndStackTrace)) {
      val monitor = PrintEventsWithStream(new PrintStream(target.mock, true), action, action, action)
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
    val target = new PrintMock
    for (action <- Seq(PrintNothing, PrintMessage, PrintMessageAndStackTrace)) {
      val monitor = PrintEventsWithWriter(new PrintWriter(target.mock, true), action, action, action)
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
  class PrintMock {
    val mock = new ByteArrayOutputStream
    /* Infers a `PrintAction` from the current value of the buffer and subsequently clears the buffer. */
    def complete(): PrintEvents.PrintAction = {
      val txt = mock.toString.trim
      mock.reset()
      import PrintEvents.PrintAction._
      if (txt.isEmpty) PrintNothing
      else if (txt.split("[\r\n]+").size <= 2) PrintMessage
      else PrintMessageAndStackTrace
    }
  }

  "EventMonitor.LogEventsWithJava" should "format and submit log entries to java.util.logging loggers" in {
    import LogEvents.LogAction._
    val logger = new LoggerMock
    for (action <- Seq(LogAt(Level.SEVERE), LogAt(Level.WARNING), LogAt(Level.INFO), LogAt(Level.CONFIG))) {
      val monitor = LogEventsWithJava(logger.mock, action, action, action)
      monitor.retrying(None, thrown, 1, 1.second, true)
      logger.isLoggable.expects(action.level).returns(true).once
      logger.log.expects(action.level, *, thrown).once
      monitor.retrying(Some("test"), thrown, 2, 1.second, false)
      logger.isLoggable.expects(action.level).returns(false).once
      monitor.interrupted(None, thrown, 3)
      logger.isLoggable.expects(action.level).returns(false).once
      monitor.interrupted(Some("test"), thrown, 4)
      logger.isLoggable.expects(action.level).returns(true).once
      logger.log.expects(action.level, *, thrown).once
      monitor.aborted(None, thrown, 5)
      logger.isLoggable.expects(action.level).returns(true).once
      logger.log.expects(action.level, *, thrown).once
      monitor.aborted(Some("test"), thrown, 6)
    }
    val monitor = LogEventsWithJava(logger.mock, LogNothing, LogNothing, LogNothing)
    monitor.retrying(None, thrown, 1, 1.second, true)
    monitor.retrying(Some("test"), thrown, 2, 1.second, false)
    monitor.interrupted(None, thrown, 3)
    monitor.interrupted(Some("test"), thrown, 4)
    monitor.aborted(None, thrown, 5)
    monitor.aborted(Some("test"), thrown, 6)
  }

  class LoggerMock { self =>
    val isLoggable = mockFunction[Level, Boolean]
    val log = mockFunction[Level, String, Throwable, Unit]
    val mock = new Logger(null, null) {
      override def isLoggable(level: Level) = self.isLoggable(level)
      override def log(level: Level, msg: String, thrown: Throwable) = self.log(level, msg, thrown)
    }
  }

  "EventMonitor.LogEventsWithSlf4j" should "format and submit log entries to Slf4j loggers" in {
    import LogEvents.LogAction._
    import LogEventsWithSlf4j.Slf4jLevel
    import LogEventsWithSlf4j.Slf4jLevel._
    val logger = mock[MockSlf4jLogger]
    def expectsIsLoggable(action: LogAt[Slf4jLevel], returns: Boolean) = action.level match {
      case Error => (logger.isErrorEnabled _).expects().returns(returns).once
      case Warn => (logger.isWarnEnabled _).expects().returns(returns).once
      case Info => (logger.isInfoEnabled _).expects().returns(returns).once
      case Debug => (logger.isDebugEnabled _).expects().returns(returns).once
      case Trace => (logger.isTraceEnabled _).expects().returns(returns).once
    }
    def expectsLog(action: LogAt[Slf4jLevel]) = action.level match {
      case Error => (logger.error _).expects(*, thrown).once
      case Warn => (logger.warn _).expects(*, thrown).once
      case Info => (logger.info _).expects(*, thrown).once
      case Debug => (logger.debug _).expects(*, thrown).once
      case Trace => (logger.trace _).expects(*, thrown).once
    }
    for (action <- Seq[LogAt[Slf4jLevel]](LogAt(Error), LogAt(Warn), LogAt(Info), LogAt(Debug), LogAt(Trace))) {
      val monitor = LogEventsWithSlf4j(logger, action, action, action)
      monitor.retrying(None, thrown, 1, 1.second, true)
      expectsIsLoggable(action, true)
      expectsLog(action)
      monitor.retrying(Some("test"), thrown, 2, 1.second, false)
      expectsIsLoggable(action, false)
      monitor.interrupted(None, thrown, 3)
      expectsIsLoggable(action, false)
      monitor.interrupted(Some("test"), thrown, 4)
      expectsIsLoggable(action, true)
      expectsLog(action)
      monitor.aborted(None, thrown, 5)
      expectsIsLoggable(action, true)
      expectsLog(action)
      monitor.aborted(Some("test"), thrown, 6)
    }
    val monitor = LogEventsWithSlf4j(logger, LogNothing, LogNothing, LogNothing)
    monitor.retrying(None, thrown, 1, 1.second, true)
    monitor.retrying(Some("test"), thrown, 2, 1.second, false)
    monitor.interrupted(None, thrown, 3)
    monitor.interrupted(Some("test"), thrown, 4)
    monitor.aborted(None, thrown, 5)
    monitor.aborted(Some("test"), thrown, 6)
  }

  /**
   * A trait that presents a narrow view of Slf4j loggers to help ScalaMock resolve the correct overloaded method.
   */
  trait MockSlf4jLogger extends Slf4jLogger {
    override def isTraceEnabled(): Boolean
    override def isDebugEnabled(): Boolean
    override def isInfoEnabled(): Boolean
    override def isWarnEnabled(): Boolean
    override def isErrorEnabled(): Boolean
    override def trace(s: String, t: Throwable): Unit
    override def debug(s: String, t: Throwable): Unit
    override def info(s: String, t: Throwable): Unit
    override def warn(s: String, t: Throwable): Unit
    override def error(s: String, t: Throwable): Unit
  }

}