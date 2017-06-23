/* RandomizedBackoff.scala
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
package atmos.backoff

import atmos.BackoffPolicy
import scala.concurrent.duration._
import scala.util.{Random, Try}

/**
 * A policy that randomizes the result of another policy by adding a random duration in the specified range.
 *
 * @param policy The base policy to randomize the result of.
 * @param range  The range of values that may be used to modify the result of the base policy.
 */
case class RandomizedBackoff(policy: BackoffPolicy, range: (FiniteDuration, FiniteDuration)) extends BackoffPolicy {

  /** The definition of the range that random values are drawn from. */
  private val (offset, scaleInNanos) = {
    val (low, high) = if (range._1 <= range._2) range._1 -> range._2 else range._2 -> range._1
    low -> (high - low).toNanos
  }

  /* Randomize the result of the underlying backoff policy. */
  override def nextBackoff(attempts: Int, outcome: Try[Any]) =
    policy.nextBackoff(attempts, outcome) + offset + (scaleInNanos * Random.nextDouble()).round.nanos

}