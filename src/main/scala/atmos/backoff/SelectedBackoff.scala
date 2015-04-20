/* SelectedBackoff.scala
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
import scala.util.Try

/**
 * A policy that delegates to another policy that is selected based on the most recently evaluated outcome.
 *
 * @param f The function that maps from outcomes to backoff policies.
 */
case class SelectedBackoff(f: Try[Any] => BackoffPolicy) extends BackoffPolicy {

  /* Return the result of the backoff policy specified by the underlying function. */
  override def nextBackoff(attempts: Int, outcome: Try[Any]) = f(outcome).nextBackoff(attempts, outcome)

}