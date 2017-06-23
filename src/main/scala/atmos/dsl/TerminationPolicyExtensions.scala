/* TerminationPolicyExtensions.scala
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
 * Adds DSL extension methods to the termination policy interface.
 *
 * @param self The termination policy to add the extension methods to.
 */
case class TerminationPolicyExtensions(self: TerminationPolicy) extends AnyVal {

  /**
   * Creates a termination policy that signals for termination only after both `self` and `that` terminate.
   *
   * @param that The other termination policy to combine with.
   */
  def && (that: TerminationPolicy): TerminationPolicy = termination.RequireBoth(self, that)

  /**
   * Creates a termination policy that signals for termination after either `self` or `that` terminate.
   *
   * @param that The other termination policy to combine with.
   */
  def || (that: TerminationPolicy): TerminationPolicy = termination.RequireEither(self, that)

}