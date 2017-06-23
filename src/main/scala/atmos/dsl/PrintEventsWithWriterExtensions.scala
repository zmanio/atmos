/* PrintEventsWithWriterExtensions.scala
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

import atmos.monitor.{EventClassifier, PrintAction, PrintEventsWithWriter}

/**
 * Adds DSL extension methods to the [[atmos.monitor.PrintEventsWithWriter]] interface.
 *
 * @param self The print writer event monitor to add the extension methods to.
 */
case class PrintEventsWithWriterExtensions(self: PrintEventsWithWriter) extends AbstractPrintEventsExtensions {

  /* Set the self type to the wrapped type. */
  override type Self = PrintEventsWithWriter

  /* Set the default retrying action. */
  override def onRetrying(action: PrintAction) = self.copy(retryingAction = action)

  /* Set the default interrupted action. */
  override def onInterrupted(action: PrintAction) = self.copy(interruptedAction = action)

  /* Set the default aborted action. */
  override def onAborted(action: PrintAction) = self.copy(abortedAction = action)

  /* Set a custom retrying action. */
  override def onRetryingWhere(classifier: EventClassifier[PrintAction]) =
    self.copy(retryingActionSelector = classifier)

  /* Set a custom interrupted action. */
  override def onInterruptedWhere(classifier: EventClassifier[PrintAction]) =
    self.copy(interruptedActionSelector = classifier)

  /* Set a custom aborted action. */
  override def onAbortedWhere(classifier: EventClassifier[PrintAction]) =
    self.copy(abortedActionSelector = classifier)

}