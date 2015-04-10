/* RetryDSLSpec.scala
 * 
 * Copyright (c) 2013 linkedin.com
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

import java.io.{ PrintWriter, StringWriter }
import java.util.logging.{ Logger, Level }
import scala.concurrent.duration._
import scala.util.Try
import akka.event.{ Logging, LoggingAdapter }
import org.slf4j.LoggerFactory
import org.scalatest._

/**
 * Test suite for [[atmos.retries.RetryDSL]].
 */
class RetryDSLSpec extends FlatSpec with Matchers {

  import atmos.dsl._
  import atmos.backoff._
  import atmos.monitor._
  import atmos.termination._
  import ErrorClassification._

  "RetryDSL" should "create retry policies by describing termination policies" in {
    neverRetry shouldEqual RetryPolicy(AlwaysTerminate)
    retrying shouldEqual RetryPolicy()
    retryFor { 5.attempts } shouldEqual RetryPolicy(LimitAttempts(5))
    retryFor { 5.minutes } shouldEqual RetryPolicy(LimitDuration(5.minutes))
    retryFor { 5.attempts && 5.minutes } shouldEqual
      RetryPolicy(RequireBoth(LimitAttempts(5), LimitDuration(5.minutes)))
    retryFor { 5.attempts || 5.minutes } shouldEqual
      RetryPolicy(RequireEither(LimitAttempts(5), LimitDuration(5.minutes)))
    retryForever shouldEqual RetryPolicy(NeverTerminate)
  }

  it should "configure retry policies with backoff policies" in {
    retrying using constantBackoff shouldEqual RetryPolicy(backoff = ConstantBackoff())
    retrying using constantBackoff(1.second) shouldEqual RetryPolicy(backoff = ConstantBackoff(1.second))
    retrying using linearBackoff shouldEqual RetryPolicy(backoff = LinearBackoff())
    retrying using linearBackoff(1.second) shouldEqual RetryPolicy(backoff = LinearBackoff(1.second))
    retrying using exponentialBackoff shouldEqual RetryPolicy(backoff = ExponentialBackoff())
    retrying using exponentialBackoff(1.second) shouldEqual RetryPolicy(backoff = ExponentialBackoff(1.second))
    retrying using fibonacciBackoff shouldEqual RetryPolicy(backoff = FibonacciBackoff())
    retrying using fibonacciBackoff(1.second) shouldEqual RetryPolicy(backoff = FibonacciBackoff(1.second))
    val selector: Try[Any] => BackoffPolicy = { case _ => LinearBackoff() }
    retrying using selectedBackoff(selector) shouldEqual RetryPolicy(backoff = SelectedBackoff(selector))
    val zero = Duration.Zero
    val min = -10.millis
    val max = 10.millis
    retrying using LinearBackoff().randomized(max) shouldEqual RetryPolicy(backoff = RandomizedBackoff(LinearBackoff(), zero -> max))
    retrying using LinearBackoff().randomized(min -> max) shouldEqual RetryPolicy(backoff = RandomizedBackoff(LinearBackoff(), min -> max))
  }

  it should "configure retry policies with event monitors" in {
    retrying monitorWith {
      System.out onRetrying printMessageAndStackTrace onInterrupted printNothing onAborted printMessage
    } shouldEqual RetryPolicy(monitor = PrintEventsWithStream(
      System.out, PrintAction.PrintMessageAndStackTrace, PrintAction.PrintNothing, PrintAction.PrintMessage))
    val writer = new PrintWriter(new StringWriter)
    retrying monitorWith {
      writer onRetrying printMessageAndStackTrace onInterrupted printNothing onAborted printMessage
    } shouldEqual RetryPolicy(monitor = PrintEventsWithWriter(
      writer, PrintAction.PrintMessageAndStackTrace, PrintAction.PrintNothing, PrintAction.PrintMessage))
    val logger = Logger.getLogger(getClass.getName)
    retrying monitorWith {
      logger onRetrying logDebug onInterrupted logNothing onAborted logWarning
    } shouldEqual RetryPolicy(monitor = LogEventsWithJava(
      logger, LogAction.LogAt(Level.CONFIG), LogAction.LogNothing, LogAction.LogAt(Level.WARNING)))
    locally {
      import AkkaSupport._
      val akka: LoggingAdapter = null
      retrying monitorWith {
        akka onRetrying logNothing onInterrupted logInfo onAborted logError
      } shouldEqual RetryPolicy(monitor = LogEventsWithAkka(
        akka, LogAction.LogNothing, LogAction.LogAt(Logging.InfoLevel), LogAction.LogAt(Logging.ErrorLevel)))
    }
    locally {
      import Slf4jSupport._
      import LogEventsWithSlf4j.Slf4jLevel
      val slf4j = LoggerFactory.getLogger(this.getClass)
      retrying monitorWith {
        slf4j onRetrying logNothing onInterrupted logInfo onAborted logError
      } shouldEqual RetryPolicy(monitor = LogEventsWithSlf4j(
        slf4j, LogAction.LogNothing, LogAction.LogAt(Slf4jLevel.Info), LogAction.LogAt(Slf4jLevel.Error)))
    }
  }

  it should "configure retry policies with error classifiers" in {
    stopRetrying shouldEqual Fatal
    keepRetrying shouldEqual Recoverable
    keepRetryingSilently shouldEqual SilentlyRecoverable
    val classifier = ErrorClassifier {
      case _: RuntimeException => stopRetrying
    }
    retrying onError classifier shouldEqual RetryPolicy(classifier = classifier)
  }

}
