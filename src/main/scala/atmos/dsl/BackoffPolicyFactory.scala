/* BackoffPolicyFactory.scala
 * 
 * Copyright (c) 2013-2014 bizo.com
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

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import atmos._

/**
 * A factory for duration-based backoff policies.
 *
 * @param self The mapping from durations to backoff policies.
 */
case class BackoffPolicyFactory(self: FiniteDuration => BackoffPolicy) {

  /**
   * Creates a backoff policy using this factory's underlying mapping.
   * 
   * @param backoff The base backoff duration to pass to the backoff policy mapping.
   */
  def apply(backoff: FiniteDuration = atmos.backoff.defaultBackoff): BackoffPolicy = self(backoff)

}

/**
 * Factory for backoff policy factories.
 */
object BackoffPolicyFactory {
  
  /**
   * An implicit view of any backoff policy factory as a backoff policy with the default base backoff duration.
   * 
   * @param factory The backoff policy factory to use to construct a default backoff policy instance.
   */
  implicit def backoffPolicyFactoryToBackoffPolicy(factory: BackoffPolicyFactory): BackoffPolicy = factory()
  
}