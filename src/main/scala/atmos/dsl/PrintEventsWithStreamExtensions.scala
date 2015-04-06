/* PrintEventsWithStreamExtensions.scala
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
package atmos.dsl

import atmos.monitor

/**
 * Adds DSL extension methods to the [[atmos.monitor.PrintEventsWithStream]] interface.
 *
 * @param self The print stream event monitor to add the extension methods to.
 */
case class PrintEventsWithStreamExtensions(self: monitor.PrintEventsWithStream) extends AnyVal {

  import monitor.PrintAction

  /** Returns a copy of the underlying monitor that prints events with the specified retrying strategy. */
  def onRetrying(action: PrintAction) = self.copy(retryingAction = action)

  /** Returns a copy of the underlying monitor that prints events with the specified interrupted strategy. */
  def onInterrupted(action: PrintAction) = self.copy(interruptedAction = action)

  /** Returns a copy of the underlying monitor that prints events with the specified aborting strategy. */
  def onAborted(action: PrintAction) = self.copy(abortedAction = action)

}