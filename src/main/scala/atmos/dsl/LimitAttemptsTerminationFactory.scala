/* LimitAttemptsTerminationFactory.scala
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

import atmos.termination

/**
 * Adds a termination policy factory named `attempts` to `Int`.
 *
 * @param self The maximum number of attempts that the resulting termination policy will specify.
 */
case class LimitAttemptsTerminationFactory(self: Int) extends AnyVal {

  /** Creates a termination policy that limits a retry operation to `self` attempts. */
  def attempts: TerminationPolicy = termination.LimitAttempts(self)

}