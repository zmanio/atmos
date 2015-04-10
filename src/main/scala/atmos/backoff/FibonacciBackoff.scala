/* FibonacciBackoff.scala
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

import scala.concurrent.duration._
import scala.util.Try

/**
 * A policy that increases the initial backoff duration by repeatedly multiplying by an approximation of the golden
 * ratio (8 / 5, the sixth and fifth fibonacci numbers).
 *
 * @param initialBackoff The backoff used for the first retry as well as the base for all subsequent retries.
 */
case class FibonacciBackoff(initialBackoff: FiniteDuration = defaultBackoff) extends atmos.BackoffPolicy {

  /** @inheritdoc */
  def nextBackoff(attempts: Int, outcome: Try[Any]) =
    (initialBackoff.toNanos * math.pow(8.0 / 5.0, attempts - 1.0)).round.nanos

}