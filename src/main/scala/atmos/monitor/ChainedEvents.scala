/* ChainedEvents.scala
 * 
 * Copyright (c) 2015 zman.io
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
package atmos.monitor

import atmos.EventMonitor
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * A monitor that will always pass invocations on to the two specified monitors.
 *
 * @param firstMonitor  The first event monitor to pass all invocations to.
 * @param secondMonitor The second event monitor to pass all invocations to.
 */
case class ChainedEvents(firstMonitor: EventMonitor, secondMonitor: EventMonitor) extends EventMonitor {

  /* Notify both monitors of the retrying event. */
  override def retrying(name: Option[String], outcome: Try[Any], attempts: Int, backoff: FiniteDuration, silent: Boolean) = {
    try {
      firstMonitor.retrying(name, outcome, attempts, backoff, silent)
    } catch {
      case thrown: Throwable =>
        try secondMonitor.retrying(name, outcome, attempts, backoff, silent) catch {case _: Throwable =>}
        throw thrown
    }
    secondMonitor.retrying(name, outcome, attempts, backoff, silent)
  }

  /* Notify both monitors of the interrupted event. */
  override def interrupted(name: Option[String], outcome: Try[Any], attempts: Int) = {
    try {
      firstMonitor.interrupted(name, outcome, attempts)
    } catch {
      case thrown: Throwable =>
        try secondMonitor.interrupted(name, outcome, attempts) catch {case _: Throwable =>}
        throw thrown
    }
    secondMonitor.interrupted(name, outcome, attempts)
  }

  /* Notify both monitors of the aborted event. */
  override def aborted(name: Option[String], outcome: Try[Any], attempts: Int) = {
    try {
      firstMonitor.aborted(name, outcome, attempts)
    } catch {
      case thrown: Throwable =>
        try secondMonitor.aborted(name, outcome, attempts) catch {case _: Throwable =>}
        throw thrown
    }
    secondMonitor.aborted(name, outcome, attempts)
  }

}