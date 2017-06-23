/* Slf4jSupport.scala
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
package atmos.dsl

import atmos.monitor
import atmos.monitor.LogEventsWithSlf4j.Slf4jLevel
import org.slf4j.Logger
import scala.language.implicitConversions

/**
 * Separate namespace for optional SLF4J support.
 */
object Slf4jSupport {

  /**
   * Creates a new event monitor that submits events to a SLF4J logger.
   *
   * @param logger The SLF4J logger to supply with event messages.
   */
  implicit def slf4jLoggerToEventMonitor(logger: Logger): monitor.LogEventsWithSlf4j =
    monitor.LogEventsWithSlf4j(logger)

  /**
   * Creates a new event monitor extension interface for a SLF4J logger.
   *
   * @param logger The logger to create a new event monitor extension interface for.
   */
  implicit def slf4jLoggerToEventMonitorExtensions(logger: Logger): LogEventsWithSlf4jExtensions =
    new LogEventsWithSlf4jExtensions(logger)

  /**
   * Provides an implicit extension of the [[atmos.monitor.LogEventsWithSlf4j]] interface.
   *
   * @param policy The Slf4j logging event monitor to extend the interface of.
   */
  implicit def logEventsWithSlf4jToLogEventsWithSlf4jExtensions //
  (policy: monitor.LogEventsWithSlf4j): LogEventsWithSlf4jExtensions =
    LogEventsWithSlf4jExtensions(policy)

  /**
   * A tag for levels provided for Slf4j.
   */
  implicit object Slf4jEventLogLevels extends EventLogLevels[Slf4jLevel] {
    override def errorLevel = Slf4jLevel.Error

    override def warningLevel = Slf4jLevel.Warn

    override def infoLevel = Slf4jLevel.Info

    override def debugLevel = Slf4jLevel.Debug
  }

}

