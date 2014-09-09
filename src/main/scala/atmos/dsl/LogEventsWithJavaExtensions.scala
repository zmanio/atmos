/* LogEventsWithJavaExtensions.scala
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
package atmos.dsl

import java.util.logging.Level
import atmos.monitor

/**
 * Exposes extensions on any instance of `monitor.LogEventsWithJava`.
 */
case class LogEventsWithJavaExtensions(self: monitor.LogEventsWithJava) extends AnyVal {

  /** Returns a copy of the underlying monitor that logs events at the specified retrying level. */
  def onRetrying(action: monitor.LogAction[Level]) = self.copy(retryingAction = action)

  /** Returns a copy of the underlying monitor that logs events at the specified interrupted level. */
  def onInterrupted(action: monitor.LogAction[Level]) = self.copy(interruptedAction = action)

  /** Returns a copy of the underlying monitor that logs events at the specified aborting level. */
  def onAborted(action: monitor.LogAction[Level]) = self.copy(abortedAction = action)

}