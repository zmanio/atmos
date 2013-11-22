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
   * Computes the next backoff duration using the specified number of attempts and/or the previous backoff duration.
   *
   * @param attempts The number of attempts that have been made so far.
   * @param previousBackoff The previous backoff duration or `Duration.Zero` when computing the initial backoff.
   */
  def nextBackoff(attempts: Int, previousBackoff: FiniteDuration): FiniteDuration

}

/**
 * Factory for common backoff strategies.
 */
object BackoffPolicy {

  /** The default backoff duration. */
  val defaultBackoff: FiniteDuration = 100.millis

  /**
   * A strategy that uses the same backoff after every attempt.
   *
   * @param backoff The backoff to use after every attempt.
   */
  case class Constant(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousBackoff: FiniteDuration) = backoff
  }

  /**
   * A strategy that increases the backoff duration linearly after every attempt.
   *
   * @param backoff The duration to add to the backoff after every attempt.
   */
  case class Linear(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousBackoff: FiniteDuration) = backoff * attempts
  }

  /**
   * A strategy that increases the backoff duration exponentially after every attempt.
   *
   * @param backoff The backoff used for the first retry and used as the base for all subsequent attempts.
   */
  case class Exponential(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousBackoff: FiniteDuration) =
      if (previousBackoff == Duration.Zero) backoff else previousBackoff * 2
  }

  /**
   * A strategy that increases the backoff duration by repeatedly multiplying by the an approximation of the golden
   * ratio (8 / 5, the sixth and fifth fibonacci numbers).
   *
   * @param backoff The backoff used for the first retry and used as the base for all subsequent attempts.
   */
  case class Fibonacci(backoff: FiniteDuration = defaultBackoff) extends BackoffPolicy {
    override def nextBackoff(attempts: Int, previousBackoff: FiniteDuration) =
      if (previousBackoff == Duration.Zero) backoff else previousBackoff * 8 / 5
  }

}