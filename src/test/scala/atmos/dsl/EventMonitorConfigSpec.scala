/* EventMonitorConfigSpec.scala
 * 
 * Copyright (c) 2015 zman.io
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
package atmos.dsl

import akka.event.{Logging => AkkaLogging, LoggingAdapter => AkkaLogger}
import atmos.monitor._
import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter}
import java.util.logging.{Level => JLevel, Logger => JLogger}
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.slf4j.{Logger => Slf4jLogger}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Test suite for the various ways to configure an [[atmos.EventMonitor]].
 */
class EventMonitorConfigSpec extends FlatSpec with Matchers with MockFactory {

  val success = Success("")
  val exception = Failure(new Exception)
  val error = Failure(new Error)

  "Print streams" should "allow for fine-grained configuration" in {
    val mockPrinln = mockFunction[String, Unit]
    val target = new PrintStream(new ByteArrayOutputStream) {
      override def println(s: String) = mockPrinln(s)
    }
    new TestCase[PrintStream, PrintEventsWithStream, PrintAction] {
      override def expect(action: PrintAction) = action match {
        case PrintAction.PrintMessage => mockPrinln expects * noMoreThanTwice
        case PrintAction.PrintMessageAndStackTrace => mockPrinln expects * anyNumberOfTimes
        case _ =>
      }
    } run(target, PrintAction.PrintNothing, PrintAction.PrintMessage, PrintAction.PrintMessageAndStackTrace)
  }

  "Print writers" should "allow for fine-grained configuration" in {
    val mockPrinln = mockFunction[String, Unit]
    val target = new PrintWriter(new ByteArrayOutputStream) {
      override def println(s: String) = mockPrinln(s)
    }
    new TestCase[PrintWriter, PrintEventsWithWriter, PrintAction] {
      override def expect(action: PrintAction) = action match {
        case PrintAction.PrintMessage => mockPrinln expects * noMoreThanTwice
        case PrintAction.PrintMessageAndStackTrace => mockPrinln expects * anyNumberOfTimes
        case _ =>
      }
    } run(target, PrintAction.PrintNothing, PrintAction.PrintMessage, PrintAction.PrintMessageAndStackTrace)
  }

  "Java loggers" should "allow for fine-grained configuration" in {
    val mockLogThrown = mockFunction[JLevel, String, Throwable, Unit]
    val target = new JLogger(null, null) {
      override def isLoggable(level: JLevel) = true

      override def log(level: JLevel, message: String, thrown: Throwable) = mockLogThrown(level, message, thrown)
    }
    new TestCase[JLogger, LogEventsWithJava, LogAction[JLevel]] {
      override def expect(action: LogAction[JLevel]) = action match {
        case LogAction.LogAt(JLevel.INFO | JLevel.WARNING) => mockLogThrown.expects(*, *, *) once
        case _ =>
      }
    } run(target, LogAction.LogNothing, LogAction.LogAt(JLevel.INFO), LogAction.LogAt(JLevel.WARNING))
  }

  "Slf4j loggers" should "allow for fine-grained configuration" in {
    import LogEventsWithSlf4j.Slf4jLevel
    import Slf4jSupport._
    val target = mock[MockSlf4jLogger]
    (target.isInfoEnabled _).expects().returns(true).anyNumberOfTimes
    (target.isWarnEnabled _).expects().returns(true).anyNumberOfTimes
    new TestCase[Slf4jLogger, LogEventsWithSlf4j, LogAction[Slf4jLevel]] {
      override def expect(action: LogAction[Slf4jLevel]) = action match {
        case LogAction.LogAt(Slf4jLevel.Info) => (target.info(_: String, _: Throwable)).expects(*, *) once
        case LogAction.LogAt(Slf4jLevel.Warn) => (target.warn(_: String, _: Throwable)) expects(*, *) once
        case _ =>
      }
    } run(target, LogAction.LogNothing, LogAction.LogAt(Slf4jLevel.Info), LogAction.LogAt(Slf4jLevel.Warn))
  }

  "Akka loggers" should "allow for fine-grained configuration" in {
    import AkkaSupport._
    val target = new AkkaLoggerFixture
    new TestCase[AkkaLogger, LogEventsWithAkka, LogAction[AkkaLogging.LogLevel]] {
      override def expect(action: LogAction[AkkaLogging.LogLevel]) = action match {
        case LogAction.LogAt(AkkaLogging.InfoLevel) => target.log.expects(AkkaLogging.InfoLevel, *) once
        case LogAction.LogAt(AkkaLogging.WarningLevel) => target.log.expects(AkkaLogging.WarningLevel, *) once
        case _ =>
      }
    } run(target.mock, LogAction.LogNothing, LogAction.LogAt(AkkaLogging.InfoLevel), LogAction.LogAt(AkkaLogging.WarningLevel))
  }

  /** A utility for testing the various events handled by monitor DSL methods. */
  trait TestCase[T, Monitor <: EventMonitor, MonitorAction] {

    val name = None: Option[String]
    val attempt = 1
    val backoff = 1 second
    val silent = false

    /** Sets an expectation for this test case. */
    def expect(action: MonitorAction): Unit

    /** Runs this test case. */
    def run(
      target: T,
      onSuccess: MonitorAction,
      onException: MonitorAction,
      onError: MonitorAction)(
      implicit ev: T => Monitor,
      evx: Monitor => AbstractEventMonitorExtensions {
        type Self = Monitor
        type Action = MonitorAction
      }) = {
      val monitor: Monitor = target
      locally {
        val test = (monitor
          .onRetrying(onSuccess)
          .onRetryingWith[Exception](onException)
          .orOnRetryingWith[Error](onError))
        expect(onSuccess)
        test.retrying(name, success, attempt, backoff, silent)
        expect(onException)
        test.retrying(name, exception, attempt, backoff, silent)
        expect(onError)
        test.retrying(name, error, attempt, backoff, silent)
      }
      locally {
        val test = (monitor
          .onInterrupted(onSuccess)
          .onInterruptedWith[Exception](onException)
          .orOnInterruptedWith[Error](onError))
        expect(onSuccess)
        test.interrupted(name, success, attempt)
        expect(onException)
        test.interrupted(name, exception, attempt)
        expect(onError)
        test.interrupted(name, error, attempt)
      }
      locally {
        val test = (monitor
          .onAborted(onSuccess)
          .onAbortedWith[Exception](onException)
          .orOnAbortedWith[Error](onError))
        expect(onSuccess)
        test.aborted(name, success, attempt)
        expect(onException)
        test.aborted(name, exception, attempt)
        expect(onError)
        test.aborted(name, error, attempt)
      }
    }

  }

  /** A trait that presents a narrow view of Akka loggers to help ScalaMock resolve the correct overloaded method. */
  trait MockSlf4jLogger extends Slf4jLogger {
    def info(s: String, t: Throwable): Unit

    def warn(s: String, t: Throwable): Unit
  }

  /** A class that presents a narrow view of Akka loggers to enable Akka DSL testing. */
  class AkkaLoggerFixture {
    self =>
    val log = mockFunction[AkkaLogging.LogLevel, String, Unit]
    val mock = new AkkaLogger {
      def isErrorEnabled = ???

      def isWarningEnabled = true

      def isInfoEnabled = true

      def isDebugEnabled = ???

      def notifyError(message: String) = ???

      def notifyError(cause: Throwable, message: String) = ???

      def notifyWarning(message: String) = ???

      def notifyInfo(message: String) = ???

      def notifyDebug(message: String) = ???

      override def log(level: AkkaLogging.LogLevel, message: String) = self.log(level, message)
    }
  }

}