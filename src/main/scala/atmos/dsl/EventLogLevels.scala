/* EventLogLevels.scala
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

/**
 * A tag for logging system specific level types, used to map generic action names to concrete logging levels.
 *
 * @param T The type of system specific logging level.
 */
trait EventLogLevels[T] {

  /** A cached action that submits error log entries. */
  lazy val errorAction = monitor.LogAction.LogAt(errorLevel)

  /** A cached action that submits warning log entries. */
  lazy val warningAction = monitor.LogAction.LogAt(warningLevel)

  /** A cached action that submits info log entries. */
  lazy val infoAction = monitor.LogAction.LogAt(infoLevel)

  /** A cached action that submits debug log entries. */
  lazy val debugAction = monitor.LogAction.LogAt(debugLevel)

  /** The concrete error level. */
  def errorLevel: T

  /** The concrete warning level. */
  def warningLevel: T

  /** The concrete info level. */
  def infoLevel: T

  /** The concrete debug level. */
  def debugLevel: T

}

/**
 * Declarations of the default logging level tags.
 */
object EventLogLevels {

  import java.util.logging.Level

  /**
   * A tag for levels used by `java.util.logging`.
   */
  implicit object JavaLogLevels extends EventLogLevels[Level] {
    override def errorLevel = Level.SEVERE

    override def warningLevel = Level.WARNING

    override def infoLevel = Level.INFO

    override def debugLevel = Level.CONFIG
  }

}