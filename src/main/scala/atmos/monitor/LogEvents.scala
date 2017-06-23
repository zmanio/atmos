/* LogEvents.scala
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
import scala.util.{Failure, Success, Try}

/**
 * Base type for event monitors that submit log entries for retry events.
 */
trait LogEvents extends atmos.EventMonitor with FormatEvents {

  /** The type of level that this event monitor submits log entries with. */
  type LevelType

  /** The action that is performed by default when a retrying event is received. */
  val retryingAction: LogAction[LevelType]

  /** The action that is performed by default when an interrupted event is received. */
  val interruptedAction: LogAction[LevelType]

  /** The action that is performed by default when an aborted event is received. */
  val abortedAction: LogAction[LevelType]

  /** The strategy used to select an action to perform for a retrying event, defaulting to `retryingAction`. */
  val retryingActionSelector: EventClassifier[LogAction[LevelType]]

  /** The strategy used to select an action to perform for an interrupted event, defaulting to `interruptedAction`. */
  val interruptedActionSelector: EventClassifier[LogAction[LevelType]]

  /** The strategy used to select an action to perform for an aborted event, defaulting to `abortedAction`. */
  val abortedActionSelector: EventClassifier[LogAction[LevelType]]

  /* Submit the event information to the logger if said event is not silent. */
  override def retrying(name: Option[String], outcome: Try[Any], attempts: Int, backoff: FiniteDuration, silent: Boolean) =
    if (!silent) {
      retryingActionSelector.applyOrElse(outcome, (_: Try[Any]) => retryingAction) match {
        case LogAction.LogAt(level) if isLoggable(level) =>
          log(level, formatRetrying(name, outcome, attempts, backoff),
            outcome match { case Success(_) => None case Failure(t) => Some(t) })
        case _ =>
      }
    }

  /* Submit the event information to the logger. */
  override def interrupted(name: Option[String], outcome: Try[Any], attempts: Int) =
    interruptedActionSelector.applyOrElse(outcome, (_: Try[Any]) => interruptedAction) match {
      case LogAction.LogAt(level) if isLoggable(level) =>
        log(level, formatInterrupted(name, outcome, attempts),
          outcome match { case Success(_) => None case Failure(t) => Some(t) })
      case _ =>
    }

  /* Submit the event information to the logger. */
  override def aborted(name: Option[String], outcome: Try[Any], attempts: Int) =
    abortedActionSelector.applyOrElse(outcome, (_: Try[Any]) => abortedAction) match {
      case LogAction.LogAt(level) if isLoggable(level) =>
        log(level, formatAborted(name, outcome, attempts),
          outcome match { case Success(_) => None case Failure(t) => Some(t) })
      case _ =>
    }

  /** Returns true if the specified level is currently loggable by the underlying logger. */
  protected def isLoggable(level: LevelType): Boolean

  /** Logs information about an event to the underlying logger. */
  protected def log(level: LevelType, message: String, thrown: Option[Throwable]): Unit

}