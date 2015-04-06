/* LogEventsWithJava.scala
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
package atmos.monitor

import java.util.logging.{ Logger, Level }

/**
 * An event monitor that formats and logs events using the `java.util.logging` framework.
 *
 * @param logger The logger that this event monitor submits to.
 * @param retryingAction The action that is performed when a retrying event is received.
 * @param interruptedAction The action that is performed when an interrupted event is received.
 * @param abortedAction The action that is performed when an aborted event is received.
 */
case class LogEventsWithJava(
  logger: Logger,
  retryingAction: LogAction[Level] = LogEventsWithJava.defaultRetryingAction,
  interruptedAction: LogAction[Level] = LogEventsWithJava.defaultInterruptedAction,
  abortedAction: LogAction[Level] = LogEventsWithJava.defaultAbortedAction)
  extends LogEvents {

  /** @inheritdoc */
  type LevelType = Level

  /** @inheritdoc */
  def isLoggable(level: Level) = logger.isLoggable(level)

  /** @inheritdoc */
  def log(level: Level, msg: String, thrown: Throwable) = logger.log(level, msg, thrown)

}

/**
 * Factory for event monitors that format and log events using the `java.util.logging` framework.
 */
object LogEventsWithJava {

  /** The default action to perform when a retrying event is received. */
  val defaultRetryingAction: LogAction[Level] = LogAction.LogAt(Level.INFO)

  /** The default action to perform when an interrupted event is received. */
  val defaultInterruptedAction: LogAction[Level] = LogAction.LogAt(Level.WARNING)

  /** The default action to perform when an aborted event is received. */
  val defaultAbortedAction: LogAction[Level] = LogAction.LogAt(Level.SEVERE)

}