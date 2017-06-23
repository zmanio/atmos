/* AbstractLogEventsExtensions.scala
 * 
 * Copyright (c) 2015 zman.io
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

import atmos.monitor.{EventClassifier, LogAction, LogEvents}

/**
 * A base class that implements DSL extension methods for all logging event monitors.
 */
trait AbstractLogEventsExtensions extends AbstractEventMonitorExtensions {

  /** The type of the log levels used by the logging system. */
  type Level

  /* Supported types all extend `LogEvents`. */
  override type Self <: LogEvents {type LevelType = Level}

  /* Always use `LogAction`. */
  override type Action = LogAction[Level]

  /* Forward the chained classifiers to `onRetryingWhere`. */
  override def orOnRetryingWhere(classifier: EventClassifier[LogAction[Level]]) =
    onRetryingWhere(self.retryingActionSelector orElse classifier)

  /* Forward the chained classifiers to `onInterruptedWhere`. */
  override def orOnInterruptedWhere(classifier: EventClassifier[LogAction[Level]]) =
    onInterruptedWhere(self.interruptedActionSelector orElse classifier)

  /* Forward the chained classifiers to `onAbortedWhere`. */
  override def orOnAbortedWhere(classifier: EventClassifier[LogAction[Level]]) =
    onAbortedWhere(self.abortedActionSelector orElse classifier)

}