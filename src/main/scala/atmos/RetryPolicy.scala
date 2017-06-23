/* RetryPolicy.scala
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

import rummage.Clock
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * A policy that enables customizable retries for arbitrary operations.
 *
 * @param termination The strategy for determining when to abort a retry operation.
 * @param backoff     The strategy used to calculate delays between retries.
 * @param monitor     The monitor that is notified of retry events.
 * @param classifier  The classifier for errors raised during retry operations. This field is deprecated and will be
 *                    used as a fallback for the `errors` classifier, which should be used instead.
 * @param results     The classifier for results returned during retry operations.
 * @param errors      The classifier for errors raised during retry operations.
 */
case class RetryPolicy(
  termination: TerminationPolicy = RetryPolicy.defaultTermination,
  backoff: BackoffPolicy = RetryPolicy.defaultBackoff,
  monitor: EventMonitor = RetryPolicy.defaultMonitor,
  @deprecated("Use `errors` instead of `classifier`.", "2.1") classifier: ErrorClassifier = RetryPolicy.defaultErrors,
  results: ResultClassifier = RetryPolicy.defaultResults,
  errors: ErrorClassifier = RetryPolicy.defaultErrors) {

  import RetryPolicy.Outcome

  /**
   * Performs the specified operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param operation The operation to repeatedly perform.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T]()(operation: => T)(implicit clock: Clock): T =
    new SyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified named operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T](name: String)(operation: => T)(implicit clock: Clock): T =
    new SyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation synchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param clock     The clock used to track time and wait out backoff delays.
   */
  def retry[T](name: Option[String])(operation: => T)(implicit clock: Clock): T =
    new SyncRetryOperation(name, operation _).run()

  /**
   * Performs the specified operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param operation The operation to repeatedly perform.
   * @param ec        The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T]()(operation: => Future[T])(implicit ec: ExecutionContext, clock: Clock): Future[T] =
    new AsyncRetryOperation(None, operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param ec        The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T](name: String)(operation: => Future[T])(implicit ec: ExecutionContext, clock: Clock): Future[T] =
    new AsyncRetryOperation(Some(name), operation _).run()

  /**
   * Performs the specified optionally named operation asynchronously, retrying according to this policy.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The optional name of the operation.
   * @param operation The operation to repeatedly perform.
   * @param ec        The execution context to retry on.
   * @param clock     The clock used to track time and schedule backoff notifications.
   */
  def retryAsync[T](name: Option[String])(operation: => Future[T])(implicit ec: ExecutionContext, clock: Clock): Future[T] =
    new AsyncRetryOperation(name, operation _).run()

  /**
   * Base representation of a single operation being retried.
   *
   * @param name The name of this operation.
   */
  private abstract class RetryOperation(name: Option[String])(implicit val clock: Clock) {

    /** The time that this retry operation started at. */
    val startedAt = clock.tick

    /** The number of times the operation has been attempted. */
    def failedAttempts: Int

    /** Sets the number of times the operation has been attempted. */
    def failedAttempts_=(failedAttempts: Int): Unit

    /** Cached copy of the `classifier` field used as a fallback for the `errors` field. */
    private val errorClassifier = errors orElse classifier

    /**
     * Analyzes the outcome of an attempt and determines the next step to take.
     *
     * @tparam T The return type of the operation being retried.
     * @param outcome The outcome of the most recent attempt.
     */
    protected def afterAttempt[T](outcome: Try[T]): Outcome[T] = outcome match {
      case Success(result) => results.applyOrElse(result, ResultClassification) match {
        case ResultClassification.Acceptable =>
          Outcome.Return(result)
        case ResultClassification.Unacceptable(status) =>
          afterAttemptFailed(outcome, Outcome.Return(result), status)
      }
      case Failure(thrown) =>
        afterAttemptFailed(outcome, Outcome.Throw(thrown), errorClassifier.applyOrElse(thrown, ErrorClassification))
    }

    /**
     * Analyzes the outcome of a failed attempt and determines the next step to take.
     *
     * @tparam T The return type of the operation being retried.
     * @param outcome   The outcome of the most recent attempt.
     * @param terminate The result to return if the retry operation should terminate.
     * @param status    The classification of the failed attempt.
     */
    private def afterAttemptFailed[T](outcome: Try[T], terminate: Outcome[T], status: ErrorClassification): Outcome[T] = {
      failedAttempts += 1
      if (status isFatal) {
        monitor.interrupted(name, outcome, failedAttempts)
        terminate
      } else {
        val nextBackoff = backoff.nextBackoff(failedAttempts, outcome)
        val nextAttemptAt = clock.tick - startedAt + nextBackoff
        if (termination.shouldTerminate(failedAttempts, nextAttemptAt)) {
          monitor.aborted(name, outcome, failedAttempts)
          terminate
        } else {
          monitor.retrying(name, outcome, failedAttempts, nextBackoff, status.isSilent)
          Outcome.Continue(nextBackoff)
        }
      }
    }

  }

  /**
   * A retry operation that runs entirely on the calling thread.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The name of this operation.
   * @param operation The operation to repeatedly perform.
   * @param c         The clock used to track time and wait out backoff delays.
   */
  private final class SyncRetryOperation[T](name: Option[String], operation: () => T)(implicit c: Clock)
    extends RetryOperation(name) {

    override var failedAttempts = 0

    /** Repeatedly performs this operation synchronously until interrupted or aborted. */
    @annotation.tailrec
    def run(): T = afterAttempt {
      try Success(operation()) catch {case thrown: Throwable => Failure(thrown)}
    } match {
      case Outcome.Continue(backoffDuration) =>
        clock.syncWait(backoffDuration)
        run()
      case Outcome.Throw(thrown) =>
        throw thrown
      case Outcome.Return(result) =>
        result
    }

  }

  /**
   * A retry operation that runs entirely on the provided execution context.
   *
   * @tparam T The return type of the operation being retried.
   * @param name      The name of this operation.
   * @param operation The operation to repeatedly perform.
   * @param ec        The execution context to retry on.
   * @param c         The clock used to track time and schedule backoff notifications.
   */
  private final class AsyncRetryOperation[T](name: Option[String], operation: () => Future[T])(
    implicit ec: ExecutionContext, c: Clock) extends RetryOperation(name) with (Try[T] => Unit) {

    @volatile override var failedAttempts = 0

    /** The ultimate outcome of this operation. */
    val promise = Promise[T]()

    /** Repeatedly performs this operation asynchronously until interrupted or aborted. */
    def run(): Future[T] = {
      try spawn() onComplete this catch {case thrown: Throwable => promise.failure(thrown)}
      promise.future
    }

    /* Respond to the completion of the future. */
    override def apply(attempt: Try[T]) = {
      var notifyOnError = true
      try {
        afterAttempt(attempt) match {
          case Outcome.Continue(backoffDuration) =>
            clock.asyncWait(backoffDuration) onComplete {
              case Success(_) =>
                try spawn() onComplete this catch {
                  case thrown: Throwable =>
                    notifyOnError = false
                    promise.failure(thrown)
                }
              case Failure(thrown) =>
                notifyOnError = false
                promise.failure(thrown)
            }
          case Outcome.Throw(thrown) =>
            notifyOnError = false
            promise.failure(thrown)
          case Outcome.Return(result) =>
            notifyOnError = false
            promise.success(result)
        }
      } catch {
        case t: Throwable if notifyOnError => promise.failure(t)
      }
    }

    /** Runs the user-supplied function and spawns an asynchronous operation. */
    private def spawn(): Future[T] =
      try operation() catch {case thrown: Throwable => Future.failed(thrown)}

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

  /** The default classifier for results returned during retry operations. */
  val defaultResults: ResultClassifier = ResultClassifier.empty

  /** The default classifier for errors raised during retry operations. */
  val defaultErrors: ErrorClassifier = ErrorClassifier.empty

  /** The default classifier for errors raised during retry operations. */
  @deprecated("Use `defaultErrors` instead of `defaultClassifier`.", "2.1")
  val defaultClassifier: ErrorClassifier = defaultErrors

  /**
   * Internal representation of the outcome of a retry attempt.
   *
   * @tparam T The return type of the operation being retried.
   */
  private sealed trait Outcome[+T]

  /**
   * Definitions of the supported outcome types.
   */
  private object Outcome {

    /**
     * An outcome that signals an operation should be retried.
     *
     * @param backoffDuration The amount of time that should be allowed to pass before retrying.
     */
    case class Continue(backoffDuration: FiniteDuration) extends Outcome[Nothing]

    /**
     * An outcome that signals an operation should terminate by throwing an exception.
     *
     * @param thrown The exception to terminate with.
     */
    case class Throw(thrown: Throwable) extends Outcome[Nothing]

    /**
     * An outcome that signals an operation should terminate by returning a result.
     *
     * @tparam T The return type of the operation being retried.
     * @param result The result to terminate with.
     */
    case class Return[T](result: T) extends Outcome[T]

  }

}