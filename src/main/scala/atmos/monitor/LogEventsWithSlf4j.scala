/* LogEventsWithSlf4j.scala
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

import org.slf4j.Logger

/**
 * An event monitor that formats and logs events using Slf4j.
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
case class LogEventsWithSlf4j(
  logger: Logger,
  retryingAction: LogAction[LogEventsWithSlf4j.Slf4jLevel] = LogEventsWithSlf4j.defaultRetryingAction,
  interruptedAction: LogAction[LogEventsWithSlf4j.Slf4jLevel] = LogEventsWithSlf4j.defaultInterruptedAction,
  abortedAction: LogAction[LogEventsWithSlf4j.Slf4jLevel] = LogEventsWithSlf4j.defaultAbortedAction,
  retryingActionSelector: EventClassifier[LogAction[LogEventsWithSlf4j.Slf4jLevel]] = EventClassifier.empty,
  interruptedActionSelector: EventClassifier[LogAction[LogEventsWithSlf4j.Slf4jLevel]] = EventClassifier.empty,
  abortedActionSelector: EventClassifier[LogAction[LogEventsWithSlf4j.Slf4jLevel]] = EventClassifier.empty)
  extends LogEvents {

  import LogEventsWithSlf4j.Slf4jLevel

  /* Use Slf4j logging levels. */
  override type LevelType = Slf4jLevel

  /* Check if the specified level is enabled in the underlying logger. */
  override def isLoggable(level: Slf4jLevel) = level match {
    case Slf4jLevel.Error => logger.isErrorEnabled()
    case Slf4jLevel.Warn => logger.isWarnEnabled()
    case Slf4jLevel.Info => logger.isInfoEnabled()
    case Slf4jLevel.Debug => logger.isDebugEnabled()
    case Slf4jLevel.Trace => logger.isTraceEnabled()
  }

  /* Submit the supplied entry to the underlying logger. */
  override def log(level: Slf4jLevel, message: String, thrown: Option[Throwable]) = thrown match {
    case Some(t) => level match {
      case Slf4jLevel.Error => logger.error(message, t)
      case Slf4jLevel.Warn => logger.warn(message, t)
      case Slf4jLevel.Info => logger.info(message, t)
      case Slf4jLevel.Debug => logger.debug(message, t)
      case Slf4jLevel.Trace => logger.trace(message, t)
    }
    case None => level match {
      case Slf4jLevel.Error => logger.error(message)
      case Slf4jLevel.Warn => logger.warn(message)
      case Slf4jLevel.Info => logger.info(message)
      case Slf4jLevel.Debug => logger.debug(message)
      case Slf4jLevel.Trace => logger.trace(message)
    }
  }

}

/**
 * Factory for event monitors that submit events to a SLF4J logger.
 */
object LogEventsWithSlf4j {

  /** The default action to perform when a retrying event is received. */
  val defaultRetryingAction: LogAction[Slf4jLevel] = LogAction.LogAt(Slf4jLevel.Info)

  /** The default action to perform when an interrupted event is received. */
  val defaultInterruptedAction: LogAction[Slf4jLevel] = LogAction.LogAt(Slf4jLevel.Warn)

  /** The default action to perform when an aborted event is received. */
  val defaultAbortedAction: LogAction[Slf4jLevel] = LogAction.LogAt(Slf4jLevel.Error)

  /**
   * Base class of the available SLF4J logging levels.
   */
  sealed trait Slf4jLevel

  /**
   * Declarations of the available SLF4J logging levels.
   */
  object Slf4jLevel {

    /** The SLF4J error logging level. */
    case object Error extends Slf4jLevel

    /** The SLF4J warn logging level. */
    case object Warn extends Slf4jLevel

    /** The SLF4J info logging level. */
    case object Info extends Slf4jLevel

    /** The SLF4J debug logging level. */
    case object Debug extends Slf4jLevel

    /** The SLF4J trace logging level. */
    case object Trace extends Slf4jLevel

  }

}