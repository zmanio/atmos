/* ExponentialBackoff.scala
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

import scala.concurrent.duration.FiniteDuration

/**
 * A policy that doubles the backoff duration after every attempt.
 *
 * @param initialBackoff The backoff used for the first retry as well as the base for all subsequent retries.
 */
case class ExponentialBackoff(initialBackoff: FiniteDuration = defaultBackoff) extends atmos.BackoffPolicy {

  /** @inheritdoc */
  def nextBackoff(attempts: Int, previousOutcome: Any) = initialBackoff * (1L << attempts - 1)

}