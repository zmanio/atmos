/* PrintEvents.scala
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
package atmos.monitor

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

/**
 * Base type for event monitors that print information about retry events as text.
 */
trait PrintEvents extends atmos.EventMonitor with FormatEvents {

  import PrintAction._

  /** The action that is performed by default when a retrying event is received. */
  val retryingAction: PrintAction

  /** The action that is performed by default when an interrupted event is received. */
  val interruptedAction: PrintAction

  /** The action that is performed by default when an aborted event is received. */
  val abortedAction: PrintAction

  /** The strategy used to select an action to perform for a retrying event, defaulting to `retryingAction`. */
  val retryingActionSelector: EventClassifier[PrintAction]

  /** The strategy used to select an action to perform for an interrupted event, defaulting to `interruptedAction`. */
  val interruptedActionSelector: EventClassifier[PrintAction]

  /** The strategy used to select an action to perform for an aborted event, defaulting to `abortedAction`. */
  val abortedActionSelector: EventClassifier[PrintAction]

  /* Print the event information if said event is not silent. */
  override def retrying(name: Option[String], outcome: Try[Any], attempts: Int, backoff: FiniteDuration, silent: Boolean) =
    if (!silent) {
      val action = retryingActionSelector.applyOrElse(outcome, (_: Try[Any]) => retryingAction)
      if (action != PrintNothing)
        printEvent(formatRetrying(name, outcome, attempts, backoff), outcome, action == PrintMessageAndStackTrace)
    }

  /* Print the event information. */
  override def interrupted(name: Option[String], outcome: Try[Any], attempts: Int) = {
    val action = interruptedActionSelector.applyOrElse(outcome, (_: Try[Any]) => interruptedAction)
    if (action != PrintNothing)
      printEvent(formatInterrupted(name, outcome, attempts), outcome, action == PrintMessageAndStackTrace)
  }

  /* Print the event information. */
  override def aborted(name: Option[String], outcome: Try[Any], attempts: Int) = {
    val action = abortedActionSelector.applyOrElse(outcome, (_: Try[Any]) => abortedAction)
    if (action != PrintNothing)
      printEvent(formatAborted(name, outcome, attempts), outcome, action == PrintMessageAndStackTrace)
  }

  /** Utility method that handles locking on the target object when printing both a message and a stack trace. */
  private def printEvent(message: String, outcome: Try[Any], printStackTrace: Boolean): Unit = outcome match {
    case Failure(t) if printStackTrace => printMessageAndStackTrace(message, t)
    case _ => printMessage(message)
  }

  /** Prints a message the to underlying target object. */
  protected def printMessage(message: String): Unit

  /** Prints a stack trace to the underlying target object. */
  protected def printMessageAndStackTrace(message: String, thrown: Throwable): Unit

}
