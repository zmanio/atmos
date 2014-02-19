/* EventMonitor.scala
 * 
 * Copyright (c) 2013 bizo.com
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
 * 
 * Portions of this code are derived from https://github.com/aboisvert/pixii
 * and https://github.com/lpryor/squishy.
 */
package atmos.retries

import java.io.{ PrintStream, PrintWriter }
import java.util.logging.{ Level, Logger }
import scala.concurrent.duration._
import org.slf4j.{ Logger => Slf4jLogger }

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

/**
 * Common event monitor implementations.
 */
object EventMonitor {

  /**
   * A monitor that ignores all events.
   */
  object IgnoreEvents extends EventMonitor {
    override def retrying //
    (name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean) = {}
    override def interrupted(name: Option[String], thrown: Throwable, attempts: Int) = {}
    override def aborted(name: Option[String], thrown: Throwable, attempts: Int) = {}
  }

  /**
   * A mix-in that formats messages from retry events.
   */
  trait FormatEvents { self => EventMonitor

    /**
     * Formats a message for a retrying event.
     *
     * @param name The name of the operation that failed if one was provided.
     * @param thrown The exception that was thrown.
     * @param attempts The number of attempts that have been made so far.
     * @param backoff The amount of time that will pass before another attempt is made.
     */
    def formatRetrying(name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration): String = {
      val op = name getOrElse "operation"
      val tpe = thrown.getClass.getName
      val msg = Option(thrown.getMessage) getOrElse ""
      s"""|Attempt $attempts of $op failed: $tpe: $msg
          |Retrying after $backoff ...""".stripMargin
    }

    /**
     * Formats a message for an interrupted event.
     *
     * @param name The name of the operation that failed if one was provided.
     * @param thrown The exception that was thrown.
     * @param attempts The number of attempts that were made.
     */
    def formatInterrupted(name: Option[String], thrown: Throwable, attempts: Int): String = {
      val op = name getOrElse "operation"
      val tpe = thrown.getClass.getName
      val msg = Option(thrown.getMessage) getOrElse ""
      s"Attempt $attempts of $op interrupted: $tpe: $msg"
    }

    /**
     * Formats a message for an aborted event.
     *
     * @param name The name of the operation that failed if one was provided.
     * @param thrown The exception that was thrown.
     * @param attempts The number of attempts that were made.
     */
    def formatAborted(name: Option[String], thrown: Throwable, attempts: Int): String = {
      val op = name getOrElse "operation"
      val tpe = thrown.getClass.getName
      val msg = Option(thrown.getMessage) getOrElse ""
      s"Too many exceptions after attempt $attempts of $op... aborting: $tpe: $msg"
    }

  }

  //
  // Monitors that print information about retry events as text.
  //

  /**
   * Base type for event monitors that print information about retry events as text.
   */
  trait PrintEvents extends EventMonitor with FormatEvents {

    import PrintEvents.PrintAction

    /** The type of object that this event monitor prints to. */
    type TargetType <: AnyRef

    /** The object that this event monitor prints to. */
    def target: TargetType

    /** The action that is performed when a retrying event is received. */
    def retryingAction: PrintAction

    /** The action that is performed when an interrupted event is received. */
    def interruptedAction: PrintAction

    /** The action that is performed when an aborted event is received. */
    def abortedAction: PrintAction

    /**
     * The object that this event monitor prints to.
     *
     * NOTE: This method is simply an alias for `target` and exists only to preserve source compatibility with previous
     * versions. This method will be removed in Atmos 2.0, users should migrate to `target`.
     */
    @deprecated("Use target", "1.3")
    def stream: TargetType = target

    /**
     * True if this event monitor will print the stack trace for retrying events.
     *
     * NOTE: This method uses `retryingAction` to calculate its value and exists only to preserve source compatibility
     * with previous versions. This method will be removed in Atmos 2.0, users should migrate to `retryingAction`.
     */
    @deprecated("Use retryingAction", "1.3")
    def printRetryingStackTrace: Boolean = retryingAction == PrintAction.PrintMessageAndStackTrace

    /**
     * True if this event monitor will print the stack trace for interrupted events.
     *
     * NOTE: This method uses `interruptedAction` to calculate its value and exists only to preserve source
     * compatibility with previous versions. This method will be removed in Atmos 2.0, users should migrate to
     * `interruptedAction`.
     */
    @deprecated("Use interruptedAction", "1.3")
    def printInterruptedStackTrace: Boolean = interruptedAction == PrintAction.PrintMessageAndStackTrace

    /**
     * True if this event monitor will print the stack trace for aborted events.
     *
     * NOTE: This method uses `abortedAction` to calculate its value and exists only to preserve source compatibility
     * with previous versions. This method will be removed in Atmos 2.0, users should migrate to `abortedAction`.
     */
    @deprecated("Use abortedAction", "1.3")
    def printAbortedStackTrace: Boolean = abortedAction == PrintAction.PrintMessageAndStackTrace

    /** @inheritdoc */
    override def retrying //
    (name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean) =
      if (!silent && retryingAction != PrintAction.PrintNothing)
        printEvent(formatRetrying(name, thrown, attempts, backoff), thrown, retryingAction == PrintAction.PrintMessage)

    /** @inheritdoc */
    override def interrupted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (interruptedAction != PrintAction.PrintNothing)
        printEvent(formatInterrupted(name, thrown, attempts), thrown, interruptedAction == PrintAction.PrintMessage)

    /** @inheritdoc */
    override def aborted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (abortedAction != PrintAction.PrintNothing)
        printEvent(formatAborted(name, thrown, attempts), thrown, abortedAction == PrintAction.PrintMessage)

    /** Utility method that handles locking on the target object when printing both a message and a stack trace. */
    private def printEvent(message: String, thrown: Throwable, noStackTrace: Boolean): Unit =
      if (noStackTrace) printMessage(message)
      else target synchronized {
        printMessage(message)
        printStackTrace(thrown)
      }

    /** Prints a message the to underlying target object. */
    protected def printMessage(message: String): Unit

    /** Prints a stack trace to the underlying target object. */
    protected def printStackTrace(thrown: Throwable): Unit

  }

  /**
   * Definitions associated with event monitors that print information about retry events as text.
   */
  object PrintEvents {

    /** The action that is performed by default when a retrying event is received. */
    val defaultRetryingAction: PrintAction = PrintAction.PrintMessage

    /** The action that is performed by default when an interrupted event is received. */
    val defaultInterruptedAction: PrintAction = PrintAction.PrintMessageAndStackTrace

    /** The action that is performed by default when an aborted event is received. */
    val defaultAbortedAction: PrintAction = PrintAction.PrintMessageAndStackTrace

    /**
     * True if the stack trace from retrying events should be printed by default.
     *
     * NOTE: This field uses `defaultRetryingAction` to calculate its value and exists only to preserve source
     * compatibility with previous versions. This method will be removed in Atmos 2.0, users should migrate to
     * `defaultRetryingAction`.
     */
    @deprecated("Use defaultRetryingAction", "1.3")
    val defaultPrintRetryingStackTrace = defaultRetryingAction == PrintAction.PrintMessageAndStackTrace

    /**
     * True if the stack trace from interrupted events should be printed by default.
     *
     * NOTE: This field uses `defaultInterruptedAction` to calculate its value and exists only to preserve source
     * compatibility with previous versions. This method will be removed in Atmos 2.0, users should migrate to
     * `defaultInterruptedAction`.
     */
    @deprecated("Use defaultInterruptedAction", "1.3")
    val defaultPrintInterruptedStackTrace = defaultInterruptedAction == PrintAction.PrintMessageAndStackTrace

    /**
     * True if the stack trace from aborted events should be printed by default.
     *
     * NOTE: This field uses `defaultAbortedAction` to calculate its value and exists only to preserve source
     * compatibility with previous versions. This method will be removed in Atmos 2.0, users should migrate to
     * `defaultAbortedAction`.
     */
    @deprecated("Use defaultAbortedAction", "1.3")
    val defaultPrintAbortedStackTrace = defaultAbortedAction == PrintAction.PrintMessageAndStackTrace

    /**
     * Creates a new event monitor that prints information about retry events as text.
     *
     * NOTE: This method simply creates a `PrintEventsWithStream` and exists only to preserve source compatibility with
     * previous versions. This method will be removed in Atmos 2.0, users should migrate to
     * `PrintEventsWithStream.apply(PrintStream, PrintAction, PrintAction, PrintAction)`.
     *
     * @param stream The object that the event monitor will print to.
     * @param printRetryingStackTrace True if the event monitor will print the stack trace for retrying events.
     * @param printInterruptedStackTrace True if the event monitor will print the stack trace for interrupted events.
     * @param printAbortedStackTrace True if the event monitor will print the stack trace for aborted events.
     */
    @deprecated("Use PrintEventsWithStream.apply(PrintStream, PrintAction, PrintAction, PrintAction)", "1.3")
    def apply(
      stream: PrintStream,
      printRetryingStackTrace: Boolean = defaultPrintRetryingStackTrace,
      printInterruptedStackTrace: Boolean = defaultPrintInterruptedStackTrace,
      printAbortedStackTrace: Boolean = defaultPrintAbortedStackTrace): PrintEventsWithStream =
      PrintEventsWithStream(stream,
        if (printRetryingStackTrace) PrintAction.PrintMessageAndStackTrace else PrintAction.PrintMessage,
        if (printInterruptedStackTrace) PrintAction.PrintMessageAndStackTrace else PrintAction.PrintMessage,
        if (printAbortedStackTrace) PrintAction.PrintMessageAndStackTrace else PrintAction.PrintMessage)

    /**
     * Base type for printing-related actions that can be performed when a retry event is received.
     */
    sealed trait PrintAction

    /**
     * Definition of the printing-related actions that can be performed when a retry event is received.
     */
    object PrintAction {

      /** A print action that will not print anything. */
      case object PrintNothing extends PrintAction

      /** A print action that will only print the formatted event message. */
      case object PrintMessage extends PrintAction

      /** A print action that will print the formatted event message and the most recent exception's stack trace. */
      case object PrintMessageAndStackTrace extends PrintAction

    }

  }

  /**
   * An event monitor that prints information about retry events to a stream.
   *
   * @param target The stream that this event monitor prints to.
   * @param retryingAction The action that is performed when a retrying event is received.
   * @param interruptedAction The action that is performed when an interrupted event is received.
   * @param abortedAction The action that is performed when an aborted event is received.
   */
  case class PrintEventsWithStream(
    override val target: PrintStream,
    override val retryingAction: PrintEvents.PrintAction = PrintEvents.defaultRetryingAction,
    override val interruptedAction: PrintEvents.PrintAction = PrintEvents.defaultInterruptedAction,
    override val abortedAction: PrintEvents.PrintAction = PrintEvents.defaultAbortedAction)
    extends PrintEvents {
    override type TargetType = PrintStream
    override protected def printMessage(message: String) = target.println(message)
    override protected def printStackTrace(thrown: Throwable) = thrown.printStackTrace(target)
  }

  /**
   * An event monitor that prints information about retry events to a writer.
   *
   * @param target The writer that this event monitor prints to.
   * @param retryingAction The action that is performed when a retrying event is received.
   * @param interruptedAction The action that is performed when an interrupted event is received.
   * @param abortedAction The action that is performed when an aborted event is received.
   */
  case class PrintEventsWithWriter(
    override val target: PrintWriter,
    override val retryingAction: PrintEvents.PrintAction = PrintEvents.defaultRetryingAction,
    override val interruptedAction: PrintEvents.PrintAction = PrintEvents.defaultInterruptedAction,
    override val abortedAction: PrintEvents.PrintAction = PrintEvents.defaultAbortedAction)
    extends PrintEvents {
    override type TargetType = PrintWriter
    override protected def printMessage(message: String) = target.println(message)
    override protected def printStackTrace(thrown: Throwable) = thrown.printStackTrace(target)
  }

  /**
   * An event monitor that formats and logs events.
   */
  case class LogEvents(
    logger: Logger,
    retryingLevel: Level = LogEvents.defaultRetryingLevel,
    interruptedLevel: Level = LogEvents.defaultInterruptedLevel,
    abortedLevel: Level = LogEvents.defaultAbortedLevel)
    extends EventMonitor with FormatEvents {

    /** @inheritdoc */
    override def retrying //
    (name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean) =
      if (!silent && retryingLevel != Level.OFF && logger.isLoggable(retryingLevel))
        logger.log(retryingLevel, formatRetrying(name, thrown, attempts, backoff), thrown)

    /** @inheritdoc */
    override def interrupted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (interruptedLevel != Level.OFF && logger.isLoggable(interruptedLevel))
        logger.log(interruptedLevel, formatInterrupted(name, thrown, attempts), thrown)

    /** @inheritdoc */
    override def aborted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (abortedLevel != Level.OFF && logger.isLoggable(abortedLevel))
        logger.log(abortedLevel, formatAborted(name, thrown, attempts), thrown)

  }

  /**
   * Factory for event monitors that submit events to a logger.
   */
  object LogEvents {

    /** The default level to log retrying events at. */
    val defaultRetryingLevel = Level.INFO

    /** The default level to log interrupted events at. */
    val defaultInterruptedLevel = Level.WARNING

    /** The default level to log aborted events at. */
    val defaultAbortedLevel = Level.SEVERE

  }

  /**
   * An event monitor that formats and logs events using SLF4J.
   */
  case class LogEventsToSlf4j(
    logger: Slf4jLogger,
    retryingLevel: LogEventsToSlf4j.Slf4jLevel = LogEventsToSlf4j.defaultRetryingLevel,
    interruptedLevel: LogEventsToSlf4j.Slf4jLevel = LogEventsToSlf4j.defaultInterruptedLevel,
    abortedLevel: LogEventsToSlf4j.Slf4jLevel = LogEventsToSlf4j.defaultAbortedLevel)
    extends EventMonitor with FormatEvents {

    import LogEventsToSlf4j._

    /** @inheritdoc */
    override def retrying //
    (name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration, silent: Boolean) =
      if (!silent && isLoggable(retryingLevel))
        log(retryingLevel, formatRetrying(name, thrown, attempts, backoff), thrown)

    /** @inheritdoc */
    override def interrupted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (isLoggable(interruptedLevel))
        log(interruptedLevel, formatInterrupted(name, thrown, attempts), thrown)

    /** @inheritdoc */
    override def aborted(name: Option[String], thrown: Throwable, attempts: Int) =
      if (isLoggable(abortedLevel))
        log(abortedLevel, formatAborted(name, thrown, attempts), thrown)

    /**
     * Returns true if the specified SLF4J level is enabled.
     *
     * @param level The SLF4J level to test.
     */
    private def isLoggable(level: Slf4jLevel) = level match {
      case Slf4jLevel.Error => logger.isErrorEnabled()
      case Slf4jLevel.Warn => logger.isWarnEnabled()
      case Slf4jLevel.Info => logger.isInfoEnabled()
      case Slf4jLevel.Debug => logger.isDebugEnabled()
      case Slf4jLevel.Trace => logger.isTraceEnabled()
      case Slf4jLevel.Off => false
    }

    /**
     * Submits a log entry at the specified level.
     *
     * @param level The SLF4J level to log at.
     * @param message The message to log.
     * @param thrown The exception to log.
     */
    private def log(level: Slf4jLevel, message: String, thrown: Throwable) = level match {
      case Slf4jLevel.Error => logger.error(message, thrown)
      case Slf4jLevel.Warn => logger.warn(message, thrown)
      case Slf4jLevel.Info => logger.info(message, thrown)
      case Slf4jLevel.Debug => logger.debug(message, thrown)
      case Slf4jLevel.Trace => logger.trace(message, thrown)
      case Slf4jLevel.Off =>
    }

  }

  /**
   * Factory for event monitors that submit events to a SLF4J logger.
   */
  object LogEventsToSlf4j {

    /** The default level to log retrying events at. */
    val defaultRetryingLevel = Slf4jLevel.Info

    /** The default level to log interrupted events at. */
    val defaultInterruptedLevel = Slf4jLevel.Warn

    /** The default level to log aborted events at. */
    val defaultAbortedLevel = Slf4jLevel.Error

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

      /** The SLF4J logging level that disables logging. */
      case object Off extends Slf4jLevel

    }

  }

}