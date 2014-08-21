/* RetryPolicy.scala
 * 
 * Copyright (c) 2013-2014 bizo.com
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
package atmos

import scala.concurrent.{ blocking, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import rummage.Timer

/**
 * A policy that enables customizable retries for arbitrary operations.
 *
 * @param termination The strategy for determining when to abort a retry operation.
 * @param backoff The strategy used to calculate delays between retries.
 * @param monitor The monitor that is notified of retry events.
 * @param classifier The classifier for errors raised during retry operations.
 */
case class RetryPolicy(
  termination: TerminationPolicy = RetryPolicy.defaultTermination,
  backoff: BackoffPolicy = RetryPolicy.defaultBackoff,
  monitor: EventMonitor = RetryPolicy.defaultMonitor,
  classifier: ErrorClassifier = RetryPolicy.defaultClassifier) {

  /**
   * Performs the specified operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param operation The operation to repeatedly perform.
   */
  def retry[T]()(operation: => T): T =
    new SyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified named operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The name of the operation.
   * @param operation The operation to repeatedly perform.
   */
  def retry[T](name: String)(operation: => T): T =
    new SyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   */
  def retry[T](name: Option[String])(operation: => T): T =
    new SyncRetryOperation(name, operation _).run()

  /**
   * Performs the specified operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param operation The operation to repeatedly perform.
   * @param ec The execution context to retry on.
   * @param t The timer to schedule backoff notifications with.
   */
  def retryAsync[T]()(operation: => Future[T])(implicit ec: ExecutionContext, t: Timer): Future[T] =
    new AsyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param ec The execution context to retry on.
   * @param t The timer to schedule backoff notifications with.
   */
  def retryAsync[T](name: String)(operation: => Future[T])(implicit ec: ExecutionContext, t: Timer): Future[T] =
    new AsyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param ec The execution context to retry on.
   * @param t The timer to schedule backoff notifications with.
   */
  def retryAsync[T](name: Option[String])(operation: => Future[T])(implicit ec: ExecutionContext, t: Timer): Future[T] =
    new AsyncRetryOperation(name, operation _).run()

  /**
   * Base representation of a single operation being retried.
   *
   * @param name The name of this operation.
   */
  private abstract class RetryOperation(name: Option[String]) {

    /** The time that this retry operation started at. */
    val startedAt = System.currentTimeMillis
    
    /** The number of times the operation has been attempted. */
    def failedAttempts: Int
    
    /** Sets the number of times the operation has been attempted. */
    def failedAttempts_=(failedAttempts: Int): Unit

    /**
     * Processes an failure encountered during an attempt.
     *
     * @param e The error that was generated.
     */
    def onFailure(e: Throwable): Unit = {
      failedAttempts += 1
      val classification = classifier.applyOrElse(e, ErrorClassification)
      if (classification.isFatal) {
        try monitor.interrupted(name, e, failedAttempts) finally reportError(e)
      } else {
        val nextBackoff = backoff.nextBackoff(failedAttempts, e)
        val nextAttemptAt = (System.currentTimeMillis - startedAt).millis + nextBackoff
        if (termination.shouldTerminate(failedAttempts, nextAttemptAt)) {
          try monitor.aborted(name, e, failedAttempts) finally reportError(e)
        } else {
          monitor.retrying(name, e, failedAttempts, nextBackoff, classification.isSilent)
          awaitBackoff(nextBackoff)
        }
      }
    }

    /**
     * Processes an error that ends the retry operation.
     *
     * @param e The error to report.
     */
    def reportError(e: Throwable): Unit

    /**
     * Waits for the specified duration before continuing.
     *
     * @param backoffDuration The duration to back off for.
     */
    def awaitBackoff(backoffDuration: FiniteDuration): Unit

  }

  /**
   * A retry operation that runs entirely on the calling thread.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The name of this operation.
   * @param operation The operation to repeatedly perform.
   */
  private class SyncRetryOperation[T](name: Option[String], operation: () => T) extends RetryOperation(name) {
    
    /** @inheritdoc */
    var failedAttempts = 0

    /** Repeatedly performs this operation. */
    def run(): T = {
      while (true) try return operation() catch { case e: Throwable => onFailure(e) }
      sys.error("unreachable")
    }

    /** @inheritdoc */
    def reportError(e: Throwable) = throw e

    /** @inheritdoc */
    def awaitBackoff(backoffDuration: FiniteDuration) = blocking { Thread.sleep(backoffDuration.toMillis) }

  }

  /**
   * A retry operation that runs entirely on the provided execution context.
   *
   * @tparam T The return type of the operation being retried.
   * @param name The name of this operation.
   * @param operation The operation to repeatedly perform.
   * @param ec The execution context to retry on.
   * @param timer The timer to schedule backoff notifications with.
   */
  private class AsyncRetryOperation[T](name: Option[String], operation: () => Future[T])(
    implicit ec: ExecutionContext, timer: Timer) extends RetryOperation(name) with (Try[T] => Unit) {

    /** The ultimate outcome of this operation. */
    val outcome = Promise[T]()
    
    /** @inheritdoc */
    @volatile var failedAttempts = 0

    /** Repeatedly performs this operation. */
    def run(): Future[T] = {
      try operation().onComplete(this) catch { case e: Throwable => onFailure(e) }
      outcome.future
    }

    /* Respond to the completion of the future. */
    def apply(result: Try[T]) = result match {
      case Success(result) => outcome.success(result)
      case Failure(e) => onFailure(e)
    }

    /** @inheritdoc */
    def reportError(e: Throwable) = outcome.failure(e)

    /** @inheritdoc */
    def awaitBackoff(backoffDuration: FiniteDuration) = (timer after backoffDuration) {
      try operation().onComplete(this) catch { case e: Throwable => onFailure(e) }
    }

  }

}

/**
 * Factory for retry policies.
 */
object RetryPolicy {

  /** The default strategy for determining when to abort a retry operation. */
  val defaultTermination: TerminationPolicy = termination.LimitAttempts()

  /** The default strategy used to calculate delays between retries. */
  val defaultBackoff: BackoffPolicy = backoff.FibonacciBackoff()

  /** The default monitor that is notified of retry events. */
  val defaultMonitor: EventMonitor = monitor.IgnoreEvents

  /** The default classifier for errors raised during retry operations. */
  val defaultClassifier: ErrorClassifier = ErrorClassifier.empty

}