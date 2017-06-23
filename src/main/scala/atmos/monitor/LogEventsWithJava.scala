/* LogEventsWithJava.scala
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

import java.util.logging.{Level, Logger}

/**
 * An event monitor that formats and logs events using the `java.util.logging` framework.
 *
 * @param logger                    The logger that this event monitor submits to.
 * @param retryingAction            The action that is performed by default when a retrying event is received.
 * @param interruptedAction         The action that is performed by default when an interrupted event is received.
 * @param abortedAction             The action that is performed by default when an aborted event is received.
 * @param retryingActionSelector    The strategy used to select an action to perform for a retrying event, defaulting to
 *                                  `retryingAction`.
 * @param interruptedActionSelector The strategy used to select an action to perform for an interrupted event,
 *                                  defaulting to `interruptedAction`.
 * @param abortedActionSelector     The strategy used to select an action to perform for an aborted event, defaulting to
 *                                  `abortedAction`.
 */
case class LogEventsWithJava(
  logger: Logger,
  retryingAction: LogAction[Level] = LogEventsWithJava.defaultRetryingAction,
  interruptedAction: LogAction[Level] = LogEventsWithJava.defaultInterruptedAction,
  abortedAction: LogAction[Level] = LogEventsWithJava.defaultAbortedAction,
  retryingActionSelector: EventClassifier[LogAction[Level]] = EventClassifier.empty,
  interruptedActionSelector: EventClassifier[LogAction[Level]] = EventClassifier.empty,
  abortedActionSelector: EventClassifier[LogAction[Level]] = EventClassifier.empty)
  extends LogEvents {

  /* Use Java logging levels. */
  override type LevelType = Level

  /* Check if the specified level is enabled in the underlying logger. */
  override def isLoggable(level: Level) = logger.isLoggable(level)

  /* Submit the supplied entry to the underlying logger. */
  override def log(level: Level, msg: String, thrown: Option[Throwable]) = thrown match {
    case Some(t) => logger.log(level, msg, t)
    case None => logger.log(level, msg)
  }

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