/* AbstractEventMonitorExtensions.scala
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

import atmos.monitor.EventClassifier
import scala.reflect.ClassTag
import scala.util.Failure

/**
 * A base class that adds DSL extension methods for the specified event monitors and actions.
 */
trait AbstractEventMonitorExtensions {

  /** The type of the underlying event monitor. */
  type Self <: EventMonitor

  /** The type of action that controls the underlying event monitor. */
  type Action

  /** The underlying event monitor. */
  def self: Self

  /** Returns a copy of the underlying monitor that prints events with the specified retrying strategy. */
  def onRetrying(action: Action): Self

  /** Returns a copy of the underlying monitor that prints events with the specified interrupted strategy. */
  def onInterrupted(action: Action): Self

  /** Returns a copy of the underlying monitor that prints events with the specified aborting strategy. */
  def onAborted(action: Action): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the retrying strategy provided by the classifier.
   */
  def onRetryingWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the interrupted strategy provided by the
   * classifier.
   */
  def onInterruptedWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the aborting strategy provided by the classifier.
   */
  def onAbortedWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the retrying strategy provided by the specified
   * classifier chained to the underlying classifier.
   */
  def orOnRetryingWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the interrupted strategy provided by the
   * specified classifier chained to the underlying classifier.
   */
  def orOnInterruptedWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the aborting strategy provided by the specified
   * classifier chained to the underlying classifier.
   */
  def orOnAbortedWhere(classifier: EventClassifier[Action]): Self

  /**
   * Returns a copy of the underlying monitor that prints events with the retrying strategy for the specified exception
   * type.
   */
  def onRetryingWith[T <: Throwable : ClassTag](action: Action): Self =
    onRetryingWhere(newWithClassifier[T](action))

  /**
   * Returns a copy of the underlying monitor that prints events with the interrupted strategy for the specified
   * exception type.
   */
  def onInterruptedWith[T <: Throwable : ClassTag](action: Action): Self =
    onInterruptedWhere(newWithClassifier[T](action))

  /**
   * Returns a copy of the underlying monitor that prints events with the aborting strategy for the specified exception
   * type.
   */
  def onAbortedWith[T <: Throwable : ClassTag](action: Action): Self =
    onAbortedWhere(newWithClassifier[T](action))

  /**
   * Returns a copy of the underlying monitor that prints events with the retrying strategy for the specified exception
   * type chained to the underlying classifier.
   */
  def orOnRetryingWith[T <: Throwable : ClassTag](action: Action): Self =
    orOnRetryingWhere(newWithClassifier[T](action))

  /**
   * Returns a copy of the underlying monitor that prints events with the interrupted strategy for the specified
   * exception type chained to the underlying classifier.
   */
  def orOnInterruptedWith[T <: Throwable : ClassTag](action: Action): Self =
    orOnInterruptedWhere(newWithClassifier[T](action))

  /**
   * Returns a copy of the underlying monitor that prints events with the aborting strategy for the specified exception
   * type chained to the underlying classifier.
   */
  def orOnAbortedWith[T <: Throwable : ClassTag](action: Action): Self =
    orOnAbortedWhere(newWithClassifier[T](action))

  /** Creates a classifier that associates an action with an exception type. */
  private def newWithClassifier[T <: Throwable : ClassTag](action: Action) = EventClassifier {
    case Failure(t) if implicitly[ClassTag[T]].runtimeClass isInstance t => action
  }

}