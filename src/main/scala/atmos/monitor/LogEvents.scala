/* LogEvents.scala
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
package atmos.monitor

import scala.concurrent.duration.FiniteDuration

/**
 * Base type for event monitors that submit log entries for retry events.
 */
trait LogEvents extends atmos.EventMonitor with FormatEvents {

  /** The type of level that this event monitor submits log entries with. */
  type LevelType

  /** The action that is performed when a retrying event is received. */
  val retryingAction: LogAction[LevelType]

  /** The action that is performed when an interrupted event is received. */
  val interruptedAction: LogAction[LevelType]

  /** The action that is performed when an aborted event is received. */
  val abortedAction: LogAction[LevelType]

  /** @inheritdoc */
  def retrying(name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean) =
    retryingAction match {
      case LogAction.LogAt(level) if !silent && isLoggable(level) =>
        log(level, formatRetrying(name, thrown, attempts, backoff), thrown)
      case _ =>
    }

  /** @inheritdoc */
  def interrupted(name: Option[String], thrown: Throwable, attempts: Int) =
    interruptedAction match {
      case LogAction.LogAt(level) if isLoggable(level) =>
        log(level, formatInterrupted(name, thrown, attempts), thrown)
      case _ =>
    }

  /** @inheritdoc */
  def aborted(name: Option[String], thrown: Throwable, attempts: Int) =
    abortedAction match {
      case LogAction.LogAt(level) if isLoggable(level) =>
        log(level, formatAborted(name, thrown, attempts), thrown)
      case _ =>
    }

  /** Returns true if the specified level is currently loggable by the underlying logger. */
  protected def isLoggable(level: LevelType): Boolean

  /** Logs information about an event to the underlying logger. */
  protected def log(level: LevelType, message: String, thrown: Throwable): Unit

}