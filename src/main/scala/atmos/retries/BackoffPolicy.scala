/* BackoffPolicy.scala
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

import scala.concurrent.duration._

/**
 * A strategy for computing a sequence of wait durations for use between retry attempts.
 */
trait BackoffPolicy {

  /**
   * Computes the next backoff duration using the specified number of attempts and the error that caused this retry
   * event to happen.
   *
   * NOTE: in Atmos 1.x this method will, by default, call `nextBackoff(Int, FiniteDuration)` in order to preserve
   * source compatibility with previous versions. To support this behavior this method will recursively reconstruct
   * the value for the `previousBackoff` parameter. As of Atmos 2.0, this method will become abstract and the
   * deprecated variation will be removed.
   *
   * @param attempts The number of attempts that have been made so far.
   * @param previousError The error that caused this retry event to happen.
   */
  def nextBackoff(attempts: Int, previousError: Throwable): FiniteDuration = {
    @annotation.tailrec
    def simulate(count: Int, previousBackoff: FiniteDuration): FiniteDuration =
      if (count <= attempts) simulate(count + 1, nextBackoff(count, previousBackoff)) else previousBackoff
    simulate(1, Duration.Zero)
  }

  /**
   * Computes the next backoff duration using the specified number of attempts and/or the previous backoff duration.
   *
   * NOTE: this method will be removed in Atmos 2.0, users should migrate to `nextBackoff(Int, Throwable)`.
   *
   * @param attempts The number of attempts that have been made so far.
   * @param previousBackoff The previous backoff duration or `Duration.Zero` when computing the initial backoff.
   */
  @deprecated(message = "Use nextBackoff(Int, Throwable)", since = "1.2")
  def nextBackoff(attempts: Int, previousBackoff: FiniteDuration): FiniteDuration = ???

}

/**
 * Factory for common backoff strategies.
 */
object BackoffPolicy {

  /** The default backoff duration. */
  val defaultBackoff: FiniteDuration = 100.millis

  /**
   * A policy that uses the same backoff after every attempt.
   *
   * @param backoff The backoff to use after every attempt.
   */
  case class Constant(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousError: Throwable) = backoff
  }

  /**
   * A policy that increases the backoff duration linearly after every attempt.
   *
   * @param backoff The duration to add to the backoff after every attempt.
   */
  case class Linear(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousError: Throwable) = backoff * attempts
  }

  /**
   * A policy that doubles the backoff duration after every attempt.
   *
   * @param backoff The backoff used for the first retry and used as the base for all subsequent attempts.
   */
  case class Exponential(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousError: Throwable) = backoff * (1L << attempts - 1)
  }

  /**
   * A policy that increases the backoff duration by repeatedly multiplying by the an approximation of the golden
   * ratio (8 / 5, the sixth and fifth fibonacci numbers).
   *
   * @param backoff The backoff used for the first retry and used as the base for all subsequent attempts.
   */
  case class Fibonacci(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousError: Throwable) =
      (backoff.toNanos * math.pow(8.0 / 5.0, attempts - 1)).round.nanos
  }

  /**
   * A policy that selects the actual target policy based on the most recently thrown exception.
   *
   * @param f The function that maps from exceptions to backoff policies.
   */
  case class Selected(f: Throwable => BackoffPolicy) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousError: Throwable) =
      f(previousError).nextBackoff(attempts, previousError)
  }

}