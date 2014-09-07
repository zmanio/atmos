/* PrintAction.scala
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