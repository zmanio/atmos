/* AkkaSupport.scala
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

import akka.event.{Logging, LoggingAdapter}
import atmos.monitor
import scala.language.implicitConversions

/**
 * Separate namespace for optional Akka support.
 */
object AkkaSupport {

  /**
   * Creates a new event monitor that submits events to an Akka logging adapter.
   *
   * @param adapter The Akka logging adapter to supply with event messages.
   */
  implicit def loggingAdapterToLogEventsWithAkka(adapter: LoggingAdapter): monitor.LogEventsWithAkka =
    monitor.LogEventsWithAkka(adapter)

  /**
   * Creates a new event monitor extension interface for an Akka logging adapter.
   *
   * @param adapter The Akka logging adapter to create a new event monitor extension interface for.
   */
  implicit def loggingAdapterToLogEventsWithAkkaExtensions(adapter: LoggingAdapter): LogEventsWithAkkaExtensions =
    LogEventsWithAkkaExtensions(adapter)

  /**
   * Provides an implicit extension of the [[atmos.monitor.LogEventsWithAkka]] interface.
   *
   * @param policy The Akka logging event monitor to extend the interface of.
   */
  implicit def logEventsWithAkkaToLogEventsWithAkkaExtensions //
  (policy: monitor.LogEventsWithAkka): LogEventsWithAkkaExtensions =
    LogEventsWithAkkaExtensions(policy)

  /**
   * A tag for levels provided for Akka.
   */
  implicit object AkkaEventLogLevels extends EventLogLevels[Logging.LogLevel] {
    override def errorLevel = Logging.ErrorLevel

    override def warningLevel = Logging.WarningLevel

    override def infoLevel = Logging.InfoLevel

    override def debugLevel = Logging.DebugLevel
  }

}