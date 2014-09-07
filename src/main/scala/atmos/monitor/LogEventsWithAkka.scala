/* LogEventsWithAkka.scala
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

import akka.event.{ Logging, LoggingAdapter }

/**
 * An event monitor that formats and logs events using the `akka.event.LoggingAdapter` framework.
 *
 * @param adapter The logging adapter that this event monitor submits to.
 * @param retryingAction The action that is performed when a retrying event is received.
 * @param interruptedAction The action that is performed when an interrupted event is received.
 * @param abortedAction The action that is performed when an aborted event is received.
 */
case class LogEventsWithAkka(
  adapter: LoggingAdapter,
  retryingAction: LogAction[Logging.LogLevel] = LogEventsWithAkka.defaultRetryingAction,
  interruptedAction: LogAction[Logging.LogLevel] = LogEventsWithAkka.defaultInterruptedAction,
  abortedAction: LogAction[Logging.LogLevel] = LogEventsWithAkka.defaultAbortedAction)
  extends LogEvents {

  /** @inheritdoc */
  type LevelType = Logging.LogLevel

  /** @inheritdoc */
  def isLoggable(level: Logging.LogLevel) = adapter.isEnabled(level)

  /** @inheritdoc */
  def log(level: Logging.LogLevel, msg: String, thrown: Throwable) = level match {
    case Logging.ErrorLevel => adapter.error(thrown, msg)
    case level => adapter.log(level, msg)
  }

}

/**
 * Factory for event monitors that submit events to an Akka logging adapter.
 */
object LogEventsWithAkka {

  /** The default action to perform when a retrying event is received. */
  val defaultRetryingAction: LogAction[Logging.LogLevel] = LogAction.LogAt(Logging.InfoLevel)

  /** The default action to perform when an interrupted event is received. */
  val defaultInterruptedAction: LogAction[Logging.LogLevel] = LogAction.LogAt(Logging.WarningLevel)

  /** The default action to perform when an aborted event is received. */
  val defaultAbortedAction: LogAction[Logging.LogLevel] = LogAction.LogAt(Logging.ErrorLevel)

}