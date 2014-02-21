/* RetryDSLSpec.scala
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

import java.io.{ PrintWriter, StringWriter }
import java.util.logging.Logger
import scala.concurrent.duration._
import org.scalatest._

/**
 * Test suite for [[atmos.retries.RetryDSL]].
 */
class RetryDSLSpec extends FlatSpec with Matchers {

  import RetryDSL._
  import BackoffPolicy._
  import ErrorClassification._
  import EventMonitor._
  import TerminationPolicy._

  "RetryDSL" should "create retry policies by describing termination policies" in {
    neverRetry shouldEqual RetryPolicy(ImmediatelyTerminate)
    retrying shouldEqual RetryPolicy()
    retryFor { 5.attempts } shouldEqual RetryPolicy(LimitNumberOfAttempts(5))
    retryFor { 5.minutes } shouldEqual RetryPolicy(LimitAmountOfTimeSpent(5.minutes))
    retryFor { 5.attempts && 5.minutes } shouldEqual
      RetryPolicy(TerminateAfterBoth(LimitNumberOfAttempts(5), LimitAmountOfTimeSpent(5.minutes)))
    retryFor { 5.attempts || 5.minutes } shouldEqual
      RetryPolicy(TerminateAfterEither(LimitNumberOfAttempts(5), LimitAmountOfTimeSpent(5.minutes)))
    retryForever shouldEqual RetryPolicy(TerminationPolicy.NeverTerminate)
  }

  it should "configure retry policies with backoff policies" in {
    retrying using constantBackoff(1.second) shouldEqual RetryPolicy(backoff = Constant(1.second))
    retrying using linearBackoff(1.second) shouldEqual RetryPolicy(backoff = Linear(1.second))
    retrying using exponentialBackoff(1.second) shouldEqual RetryPolicy(backoff = Exponential(1.second))
    retrying using fibonacciBackoff(1.second) shouldEqual RetryPolicy(backoff = Fibonacci(1.second))
    val selector: Throwable => BackoffPolicy = { case _ => Linear() }
    retrying using selectedBackoff(selector) shouldEqual RetryPolicy(backoff = Selected(selector))
    val zero = Duration.Zero
    val min = -10.millis
    val max = 10.millis
    retrying using Linear().randomized(max) shouldEqual RetryPolicy(backoff = Randomized(Linear(), zero -> max))
    retrying using Linear().randomized(min -> max) shouldEqual RetryPolicy(backoff = Randomized(Linear(), min -> max))
  }

  it should "configure retry policies with event monitors" in {
    import PrintEvents.PrintAction
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
    retrying monitorWith logger shouldEqual RetryPolicy(monitor = LogEvents(logger))
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