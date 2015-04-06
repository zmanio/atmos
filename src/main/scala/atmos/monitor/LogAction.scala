/* LogAction.scala
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

/**
 * Base type for logging-related actions that can be performed when a retry event is received.
 */
sealed trait LogAction[+T]

/**
 * Definition of the logging-related actions that can be performed when a retry event is received.
 */
object LogAction {

  /** A log action that will not log anything. */
  case object LogNothing extends LogAction[Nothing]

  /** A log action that will submit a log entry at the specified level. */
  case class LogAt[T](level: T) extends LogAction[T]

}