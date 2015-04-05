/* EventMonitor.scala
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
package atmos

import scala.concurrent.duration.FiniteDuration

/**
 * A monitor that is notified of events that occur while a retry operation is in progress.
 */
trait EventMonitor {

  /**
   * Called when an operation has failed with a non-fatal error and will be retried.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that have been made so far.
   * @param backoff The amount of time that will pass before another attempt is made.
   * @param silent True if the exception was classified as silent.
   */
  def retrying(name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean): Unit

  /**
   * Called when an operation has failed with a fatal error and will not be retried.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that were made.
   */
  def interrupted(name: Option[String], thrown: Throwable, attempts: Int): Unit

  /**
   * Called when an operation has failed too many times and will not be retried.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that were made.
   */
  def aborted(name: Option[String], thrown: Throwable, attempts: Int): Unit

}