package atmos.dsl

import atmos.monitor
import com.typesafe.scalalogging.Logger

/**
 * Separate namespace for optional Scala Logging support.
 */
object ScalaLoggingSupport {

  /**
   * Creates a new event monitor that submits events to a Scala logger.
   *
   * @param logger The Scala logger to supply with event messages.
   */
  implicit def scalaLoggerToEventMonitor(logger: Logger): monitor.LogEventsWithSlf4j =
    monitor.LogEventsWithSlf4j(logger.underlying)

  /**
   * Creates a new event monitor extension interface for a Scala logger.
   *
   * @param logger The logger to create a new event monitor extension interface for.
   */
  implicit def scalaLoggerToEventMonitorExtensions(logger: Logger): LogEventsWithSlf4jExtensions =
    new LogEventsWithSlf4jExtensions(logger)


}


