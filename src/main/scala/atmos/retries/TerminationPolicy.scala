/* TerminationPolicy.scala
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
 * Strategy for determining when to abort a retry operation.
 */
trait TerminationPolicy { self =>

  /**
   * Returns true if the retry operation with the specified properties should terminate.
   *
   * @param attempts The number of attempts that have been made so far.
   * @param nextAttemptAt The amount of time between when the retry operation began and the next attempt will occur.
   */
  def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration): Boolean

}

/**
 * A factory for termination policies.
 */
object TerminationPolicy {
  
  /** The default maximum number of attempts that can be performed. */
  val defaultMaxAttempts = 3
  
  /** The default maximum duration that a retry operation should not exceed. */
  val defaultMaxDuration = 10.seconds

  /**
   * A termination strategy that never signals for termination.
   */
  case object NeverTerminate extends TerminationPolicy {
    override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) = false
  }

  /**
   * A termination strategy that limits the number of attempts made.
   *
   * @param maxAttempts The maximum number of attempts that can be performed.
   */
  case class LimitNumberOfAttempts(maxAttempts: Int = defaultMaxAttempts) extends TerminationPolicy {
    override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) = attempts >= maxAttempts
  }

  /**
   * A termination strategy that limits the amount of time spent retrying.
   *
   * @param maxDuration The maximum duration that a retry operation should not exceed.
   */
  case class LimitAmountOfTimeSpent(maxDuration: FiniteDuration = defaultMaxDuration) extends TerminationPolicy {
    override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) = nextAttemptAt >= maxDuration
  }

  /**
   * A termination strategy that signals for termination after both of the specified policies terminate.
   *
   * @param first The first of the two policies that must signal for termination.
   * @param second The second of the two policies that must signal for termination.
   */
  case class TerminateAfterBoth(first: TerminationPolicy, second: TerminationPolicy) extends TerminationPolicy {
    override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) =
      first.shouldTerminate(attempts, nextAttemptAt) && second.shouldTerminate(attempts, nextAttemptAt)
  }

  /**
   * A termination strategy that signals for termination after either of the specified policies terminate.
   *
   * @param first The first of the two policies that may signal for termination.
   * @param second The second of the two policies that may signal for termination.
   */
  case class TerminateAfterEither(first: TerminationPolicy, second: TerminationPolicy) extends TerminationPolicy {
    override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) =
      first.shouldTerminate(attempts, nextAttemptAt) || second.shouldTerminate(attempts, nextAttemptAt)
  }

}