/* BackoffPolicyExtensions.scala
 * 
 * Copyright (c) 2013-2014 linkedin.com
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
package atmos.dsl

import atmos.backoff
import scala.concurrent.duration._

/**
 * Adds DSL extension methods to the backoff policy interface.
 *
 * @param self The backoff policy to add the extension methods to.
 */
case class BackoffPolicyExtensions(self: BackoffPolicy) extends AnyVal {

  /**
   * Creates a backoff policy that randomizes the result of `self`.
   *
   * @param bound The minimum (if negative) or maximum (if positive) value in the range that may be used to modify the
   *              result of `self`.
   */
  def randomized(bound: FiniteDuration): BackoffPolicy = backoff.RandomizedBackoff(self, Duration.Zero -> bound)

  /**
   * Creates a backoff policy that randomizes the result of `self`.
   *
   * @param range The range of values that may be used to modify the result of `self`.
   */
  def randomized(range: (FiniteDuration, FiniteDuration)): BackoffPolicy = backoff.RandomizedBackoff(self, range)

}