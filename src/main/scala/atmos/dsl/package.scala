/* package.scala
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
package atmos

import java.io.{PrintStream, PrintWriter}
import rummage.{Clock, Deadlines}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.Try

/**
 * The `atmos.dsl` package defines a domain specific language for constructing and using retry policies.
 */
package object dsl extends Deadlines {

  //
  // Aliases for the top-level API.
  //

  /** An alias to the `RetryPolicy` type. */
  type RetryPolicy = atmos.RetryPolicy

  /** An alias to the `RetryPolicy` companion. */
  val RetryPolicy = atmos.RetryPolicy

  /** An alias to the `TerminationPolicy` type. */
  type TerminationPolicy = atmos.TerminationPolicy

  /** An alias to the `BackoffPolicy` type. */
  type BackoffPolicy = atmos.BackoffPolicy

  /** An alias to the `EventMonitor` type. */
  type EventMonitor = atmos.EventMonitor

  /** An alias to the `ResultClassification` type. */
  type ResultClassification = atmos.ResultClassification

  /** An alias to the `ResultClassification` companion. */
  val ResultClassification = atmos.ResultClassification

  /** An alias to the `ResultClassifier` type. */
  type ResultClassifier = atmos.ResultClassifier

  /** An alias to the `ResultClassifier` companion. */
  val ResultClassifier = atmos.ResultClassifier

  /** An alias to the `ErrorClassification` type. */
  type ErrorClassification = atmos.ErrorClassification

  /** An alias to the `ErrorClassification` companion. */
  val ErrorClassification = atmos.ErrorClassification

  /** An alias to the `ErrorClassifier` type. */
  type ErrorClassifier = atmos.ErrorClassifier

  /** An alias to the `ErrorClassifier` companion. */
  val ErrorClassifier = atmos.ErrorClassifier

  //
  // Retry policy factories and extensions.
  //

  /** Creates a new default retry policy. */
  def retrying: RetryPolicy = RetryPolicy()

  /** Creates a new retry policy that immediately terminates. */
  def neverRetry: RetryPolicy = RetryPolicy(termination.AlwaysTerminate)

  /** Creates a new retry policy that never terminates. */
  def retryForever: RetryPolicy = RetryPolicy(termination.NeverTerminate)

  /**
   * Creates a new retry policy based on the specified termination policy.
   *
   * @param termination The termination policy that will be used by the new retry policy.
   */
  def retryFor(termination: TerminationPolicy): RetryPolicy = RetryPolicy(termination)

  /**
   * Provides an implicit extension of the retry policy interface.
   *
   * @param policy The retry policy to extend the interface of.
   */
  implicit def retryPolicyToRetryPolicyExtensions(policy: RetryPolicy): RetryPolicyExtensions =
    RetryPolicyExtensions(policy)

  //
  // Termination policy factories and extensions.
  //

  /**
   * Creates a termination policy that limits a retry operation to the specified time frame for use in expressions like
   * `retryFor { 5.minutes }`.
   *
   * @param duration The maximum duration that the resulting termination policy will specify.
   */
  implicit def finiteDurationToTerminationPolicy(duration: FiniteDuration): TerminationPolicy =
    termination.LimitDuration(duration)

  /**
   * Adds logical and and or operators to durations for use in expressions like `retryFor { 5.minutes || 5.attempts }`.
   *
   * @param duration The maximum duration that the resulting termination policy will specify.
   */
  implicit def finiteDurationToTerminationPolicyExtensions(duration: FiniteDuration): TerminationPolicyExtensions =
    TerminationPolicyExtensions(duration)

  /**
   * Provides an implicit factory named `attempts` to `Int` for use in expressions like `retryFor { 5.attempts }`.
   *
   * @param attempts The maximum number of attempts that resulting termination policies will specify.
   */
  implicit def intToLimitAttemptsTerminationPolicyFactory(attempts: Int): LimitAttemptsTerminationFactory =
    LimitAttemptsTerminationFactory(attempts)

  /**
   * Provides an implicit extension of the termination policy interface.
   *
   * @param policy The termination policy to extend the interface of.
   */
  implicit def terminationPolicyToTerminationPolicyExtensions(policy: TerminationPolicy): TerminationPolicyExtensions =
    TerminationPolicyExtensions(policy)

  //
  // Backoff policy factories and extensions.
  //

  /**
   * Creates a backoff policy that uses the same backoff after every attempt.
   */
  def constantBackoff: BackoffPolicyFactory = BackoffPolicyFactory(backoff.ConstantBackoff)

  /**
   * Creates a backoff policy that increases the backoff duration linearly after every attempt.
   */
  def linearBackoff: BackoffPolicyFactory = BackoffPolicyFactory(backoff.LinearBackoff)

  /**
   * Creates a backoff policy that increases the backoff duration exponentially after every attempt.
   */
  def exponentialBackoff: BackoffPolicyFactory = BackoffPolicyFactory(backoff.ExponentialBackoff)

  /**
   * Creates a backoff policy that increases the backoff duration by repeatedly multiplying by the an approximation of
   * the golden ratio (8 / 5, the sixth and fifth fibonacci numbers).
   */
  def fibonacciBackoff: BackoffPolicyFactory = BackoffPolicyFactory(backoff.FibonacciBackoff)

  /**
   * Creates a backoff policy selects another policy based on the most recently evaluated outcome.
   *
   * @param f The function that maps from outcomes to backoff policies.
   */
  def selectedBackoff(f: Try[Any] => BackoffPolicy): BackoffPolicy = backoff.SelectedBackoff(f)

  /**
   * Provides an implicit extension of the backoff policy interface.
   *
   * @param policy The backoff policy to extend the interface of.
   */
  implicit def backoffPolicyToBackoffPolicyExtensions(policy: BackoffPolicy): BackoffPolicyExtensions =
    BackoffPolicyExtensions(policy)

  //
  // Print monitor factories and extensions.
  //

  /** Returns a print action that will print no text. */
  def printNothing: monitor.PrintAction = monitor.PrintAction.PrintNothing

  /** Returns a print action that will print only an event message. */
  def printMessage: monitor.PrintAction = monitor.PrintAction.PrintMessage

  /** Returns a print action that will print an event message and stack trace. */
  def printMessageAndStackTrace: monitor.PrintAction = monitor.PrintAction.PrintMessageAndStackTrace

  /**
   * Creates a new event monitor that prints messages to a stream.
   *
   * @param stream The stream to print events to.
   */
  implicit def printStreamToPrintEventsWithStream(stream: PrintStream): monitor.PrintEventsWithStream =
    monitor.PrintEventsWithStream(stream)

  /**
   * Creates a new event monitor extension interface for a print stream.
   *
   * @param stream The print stream to create a new event monitor extension interface for.
   */
  implicit def printStreamToPrintEventsWithStreamExtensions(stream: PrintStream): PrintEventsWithStreamExtensions =
    PrintEventsWithStreamExtensions(stream)

  /**
   * Provides an implicit extension of the [[atmos.monitor.PrintEventsWithStream]] interface.
   *
   * @param policy The print stream event monitor to extend the interface of.
   */
  implicit def printEventsWithStreamToPrintEventsWithStreamExtensions //
  (policy: monitor.PrintEventsWithStream): PrintEventsWithStreamExtensions =
    PrintEventsWithStreamExtensions(policy)

  /**
   * Creates a new event monitor that prints messages to a writer.
   *
   * @param writer The writer to print events to.
   */
  implicit def printWriterToPrintEventsWithWriter(writer: PrintWriter): monitor.PrintEventsWithWriter =
    monitor.PrintEventsWithWriter(writer)

  /**
   * Creates a new event monitor extension interface for a print writer.
   *
   * @param writer The print writer to create a new event monitor extension interface for.
   */
  implicit def printWriterToPrintEventsWithWriterExtensions(writer: PrintWriter): PrintEventsWithWriterExtensions =
    PrintEventsWithWriterExtensions(writer)

  /**
   * Provides an implicit extension of the [[atmos.monitor.PrintEventsWithWriter]] interface.
   *
   * @param policy The print writer event monitor to extend the interface of.
   */
  implicit def printEventsWithWriterToPrintEventsWithWriterExtensions //
  (policy: monitor.PrintEventsWithWriter): PrintEventsWithWriterExtensions =
    PrintEventsWithWriterExtensions(policy)

  //
  // Logging monitor factories and extensions.
  //

  /** Returns a log action that will not log anything. */
  def logNothing = monitor.LogAction.LogNothing

  /** Returns a log action that will submit a log entry at an error-equivalent level. */
  def logError[T: EventLogLevels] = implicitly[EventLogLevels[T]].errorAction

  /** Returns a log action that will submit a log entry at a warning-equivalent level. */
  def logWarning[T: EventLogLevels] = implicitly[EventLogLevels[T]].warningAction

  /** Returns a log action that will submit a log entry at an info-equivalent level. */
  def logInfo[T: EventLogLevels] = implicitly[EventLogLevels[T]].infoAction

  /** Returns a log action that will submit a log entry at a debug-equivalent level. */
  def logDebug[T: EventLogLevels] = implicitly[EventLogLevels[T]].debugAction

  /**
   * Creates a new event monitor that submits events to a logger.
   *
   * @param logger The logger to supply with event messages.
   */
  implicit def loggerToLogEventsWithJava(logger: java.util.logging.Logger): monitor.LogEventsWithJava =
    monitor.LogEventsWithJava(logger)

  /**
   * Creates a new event monitor extension interface for a logger.
   *
   * @param logger The logger to create a new event monitor extension interface for.
   */
  implicit def loggerToLogEventsWithJavaExtensions(logger: java.util.logging.Logger): LogEventsWithJavaExtensions =
    LogEventsWithJavaExtensions(logger)

  /**
   * Provides an implicit extension of the [[atmos.monitor.LogEventsWithJava]] interface.
   *
   * @param policy The java logging event monitor to extend the interface of.
   */
  implicit def logEventsWithJavaToLogEventsWithJavaExtensions //
  (policy: monitor.LogEventsWithJava): LogEventsWithJavaExtensions =
    LogEventsWithJavaExtensions(policy)

  //
  // Result classification factories.
  //

  /** Returns the `Acceptable` result classification. */
  def acceptResult: ResultClassification = ResultClassification.Acceptable

  /** Returns a factory for an `Unacceptable` result classification with an optional status. */
  def rejectResult: ResultRejection = ResultRejection

  //
  // Error classification factories.
  //

  /** Returns the `Fatal` error classification. */
  def stopRetrying: ErrorClassification = ErrorClassification.Fatal

  /** Returns the `Recoverable` error classification. */
  def keepRetrying: ErrorClassification = ErrorClassification.Recoverable

  /** Returns the `SilentlyRecoverable` error classification. */
  def keepRetryingSilently: ErrorClassification = ErrorClassification.SilentlyRecoverable

  //
  // Retry operations.
  //

  /**
   * Performs the specified operation synchronously, retrying according to the implicit retry policy.
   *
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T]()(operation: => T)(implicit policy: RetryPolicy, clock: Clock): T =
    policy.retry()(operation)

  /**
   * Performs the specified named operation synchronously, retrying according to the implicit retry policy.
   *
   * @param name      The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T](name: String)(operation: => T)(implicit policy: RetryPolicy, clock: Clock): T =
    policy.retry(name)(operation)

  /**
   * Performs the specified optionally named operation synchronously, retrying according to the implicit retry policy.
   *
   * @param name      The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T](name: Option[String])(operation: => T)(implicit policy: RetryPolicy, clock: Clock): T =
    policy.retry(name)(operation)

  /**
   * Performs the specified operation asynchronously, retrying according to the implicit retry policy.
   *
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param context   The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T]()(operation: => Future[T]) //
    (implicit policy: RetryPolicy, context: ExecutionContext, clock: Clock): Future[T] =
    policy.retryAsync()(operation)

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to the implicit retry policy.
   *
   * @param name      The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param context   The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T](name: String)(operation: => Future[T]) //
    (implicit policy: RetryPolicy, context: ExecutionContext, clock: Clock): Future[T] =
    policy.retryAsync(name)(operation)

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to the implicit retry policy.
   *
   * @param name      The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param policy    The retry policy to execute with.
   * @param context   The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T](name: Option[String])(operation: => Future[T]) //
    (implicit policy: RetryPolicy, context: ExecutionContext, clock: Clock): Future[T] =
    policy.retryAsync(name)(operation)

}