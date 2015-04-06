/* BackoffPolicy.scala
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
package atmos

import scala.concurrent.duration.FiniteDuration

/**
 * A strategy for computing a sequence of wait durations for use between retry attempts.
 */
trait BackoffPolicy {

  /**
   * Computes the next backoff duration using the specified number of attempts and the error that caused this retry
   * event to happen.
   *
   * @param attempts The number of attempts that have been made so far.
   * @param previousError The error that caused this retry event to happen.
   */
  def nextBackoff(attempts: Int, previousError: Throwable): FiniteDuration

}
