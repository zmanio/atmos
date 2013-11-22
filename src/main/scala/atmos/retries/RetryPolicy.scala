/* RetryPolicy.scala
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

import scala.concurrent.{ blocking, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import atmos.utils.Timer

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
   * @param operation The operation to repeatedly perform.
   */
  def retry[T]()(operation: => T): T = new SyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified named operation synchronously, retrying according to this policy.
   *
   * @param name The name of the operation.
   * @param operation The operation to repeatedly perform.
   */
  def retry[T](name: String)(operation: => T): T = new SyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation synchronously, retrying according to this policy.
   *
   * @param name The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   */
  def retry[T](name: Option[String])(operation: => T): T = new SyncRetryOperation(name, operation _).run()

  /**
   * Performs the specified operation asynchronously, retrying according to this policy.
   *
   * @param operation The operation to repeatedly perform.
   * @param context The execution context to retry on.
   */
  def retryAsync[T]()(operation: => Future[T])(implicit context: ExecutionContext): Future[T] =
    new AsyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @param name The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param context The execution context to retry on.
   */
  def retryAsync[T](name: String)(operation: => Future[T])(implicit context: ExecutionContext): Future[T] =
    new AsyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @param name The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param context The execution context to retry on.
   */
  def retryAsync[T](name: Option[String])(operation: => Future[T])(implicit context: ExecutionContext): Future[T] =
    new AsyncRetryOperation(name, operation _).run()

  /**
   * Representation of a single operation.
   *
   * @param name The option name of this operation.
   */
  private trait RetryOperation {

    /** The time that this retry operation started at. */
    private val startAt = System.currentTimeMillis
    /** The number of times the operation has been attempted. */
    private var failedAttempts = 0
    /** The most recently calculated backoff duration. */
    private var mostRecentBackoff: FiniteDuration = Duration.Zero

    /** Returns the name of this operation. */
    def name: Option[String]

    /**
     * Processes an error generated during an attempt.
     *
     * @param e The error that was generated.
     */
    def onFailure(e: Throwable): Unit = {
      failedAttempts += 1
      val classification = classifier.applyOrElse(e, ErrorClassification)
      if (classification.isFatal) {
        try monitor.interrupted(name, e, failedAttempts) finally reportError(e)
      } else {
        mostRecentBackoff = backoff.nextBackoff(failedAttempts, mostRecentBackoff)
        val nextAttemptAt = (System.currentTimeMillis - startAt).millis + mostRecentBackoff
        if (termination.shouldTerminate(failedAttempts, nextAttemptAt)) {
          try monitor.aborted(name, e, failedAttempts) finally reportError(e)
        } else {
          monitor.retrying(name, e, failedAttempts, mostRecentBackoff, classification.isSilent)
          awaitBackoff(mostRecentBackoff)
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
   * @param name The name of this operation.
   * @param operation The operation to repeatedly perform.
   */
  private class SyncRetryOperation[T](val name: Option[String], operation: () => T) extends RetryOperation {

    /** Repeatedly performs this operation. */
    def run(): T = {
      while (true) try return operation() catch { case e: Throwable => onFailure(e) }
      sys.error("unreachable")
    }

    /** @inheritdoc */
    override def reportError(e: Throwable) = throw e

    /** @inheritdoc */
    override def awaitBackoff(backoffDuration: FiniteDuration) = blocking(Thread.sleep(backoffDuration.toMillis))

  }

  /**
   * A retry operation that runs entirely on the provided execution context.
   *
   * @param name The name of this operation.
   * @param operation The operation to repeatedly perform.
   */
  private class AsyncRetryOperation[T](
    val name: Option[String], operation: () => Future[T])(implicit ec: ExecutionContext)
    extends RetryOperation with (Try[T] => Unit) {

    /** The ultimate outcome of this operation. */
    private val outcome = Promise[T]()

    /** Repeatedly performs this operation. */
    def run(): Future[T] = {
      try operation().onComplete(this) catch { case e: Throwable => onFailure(e) }
      outcome.future
    }

    /* Respond to the completion of the future. */
    override def apply(result: Try[T]) = result match {
      case Success(result) => outcome.success(result)
      case Failure(e) => onFailure(e)
    }

    /** @inheritdoc */
    override def reportError(e: Throwable) = outcome.failure(e)

    /** @inheritdoc */
    override def awaitBackoff(backoffDuration: FiniteDuration) = Timer.after(backoffDuration) {
      try operation().onComplete(this) catch { case e: Throwable => onFailure(e) }
    }

  }

}

/**
 * Factory for retry policies.
 */
object RetryPolicy {

  /** The default strategy for determining when to abort a retry operation. */
  val defaultTermination = TerminationPolicy.LimitNumberOfAttempts()

  /** The default strategy used to calculate delays between retries. */
  val defaultBackoff = BackoffPolicy.Fibonacci()

  /** The default monitor that is notified of retry events. */
  val defaultMonitor = EventMonitor.IgnoreEvents

  /** The default classifier for errors raised during retry operations. */
  val defaultClassifier = ErrorClassifier.empty

}