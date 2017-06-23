/* RequireBoth.scala
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
package atmos.termination

import atmos.TerminationPolicy
import scala.concurrent.duration.FiniteDuration

/**
 * A termination policy that signals for termination after both of the specified policies terminate.
 *
 * @param first  The first of the two policies that must signal for termination.
 * @param second The second of the two policies that must signal for termination.
 */
case class RequireBoth(first: TerminationPolicy, second: TerminationPolicy) extends TerminationPolicy {

  /* Signal for termination when both of the underlying policies do so. */
  override def shouldTerminate(attempts: Int, nextAttemptAt: FiniteDuration) =
    first.shouldTerminate(attempts, nextAttemptAt) && second.shouldTerminate(attempts, nextAttemptAt)

}