/* TerminationPolicy.scala
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
 * Strategy for determining when to abort a retry operation.
 */
trait TerminationPolicy {

  /**
   * Returns true if the retry operation with the specified properties should terminate.
   *
   * @param attempts      The number of attempts that have been made so far.
   * @param nextAttemptAt The duration between when the retry operation began and when the next attempt will occur.
   */
  def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration): Boolean

}