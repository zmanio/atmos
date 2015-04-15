/* RetryPolicyExtensions.scala
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

import atmos.monitor.ChainedEvents

/**
 * Adds DSL extension methods to the retry policy interface.
 *
 * @param self The retry policy to add the extension methods to.
 */
case class RetryPolicyExtensions(self: RetryPolicy) extends AnyVal {

  /**
   * Creates a new retry policy by replacing the underlying policy's termination policy.
   *
   * @param termination The termination policy to use.
   */
  def retryFor(termination: TerminationPolicy) = self.copy(termination = termination)

  /**
   * Creates a new retry policy by replacing the underlying policy's backoff policy.
   *
   * @param backoff The backoff policy to use.
   */
  def using(backoff: BackoffPolicy) = self.copy(backoff = backoff)

  /**
   * Creates a new retry policy by replacing the underlying policy's monitor.
   *
   * @param monitor The monitor to use.
   */
  def monitorWith(monitor: EventMonitor) = self.copy(monitor = monitor)

  /**
   * Creates a new retry policy by chaining the specified event monitor to the underlying policy's monitor.
   *
   * @param monitor The monitor to chain to the underlying policy's monitor.
   */
  def alsoMonitorWith(monitor: EventMonitor) = self.copy(monitor = ChainedEvents(self.monitor, monitor))

  /**
   * Creates a new retry policy by replacing the underlying policy's result classifier.
   *
   * @param results The result classifier to use.
   */
  def onResult(results: ResultClassifier) = self.copy(results = results)

  /**
   * Creates a new retry policy by replacing the underlying policy's error classifier.
   *
   * @param errors The error classifier to use.
   */
  def onError(errors: ErrorClassifier) = self.copy(errors = errors)

}