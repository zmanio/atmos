/* Timer.scala
 * 
 * Copyright (c) 2013 bizo.com
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
package atmos.utils

import akka.actor.{ ActorSystem, Cancellable }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

/**
 * A utility for scheduling tasks to run in the future.
 * 
 * This interface serves three primary use cases:
 * {{{
 * // Running an action once after a specified initial delay.
 * Timer.after(3.minutes) { println("The future!") }
 * 
 * // Running an action repeatedly with the same initial and subsequent delays.
 * Timer.every(3.minutes) { println("The future continues!") }
 * 
 * // Running an action repeatedly with the different initial and subsequent delays.
 * Timer.after(10.seconds).thenEvery(3.minutes) { println("The future never ends!") }
 * 
 * // Tasks can also be cancelled.
 * val task = Timer.every(10.seconds) { println("Only once!") }
 * Timer.after(15.seconds) { task.cancel() }
 * }}}
 */
object Timer {

  /** The scheduler that tasks are submitted to. */
  private val scheduler = ActorSystem("atmos-utils-timer").scheduler

  /**
   * Creates a builder that can perform a task once or repeatedly after the specified delay.
   *
   * @param delay The amount of time to wait before performing the task.
   */
  def after[T](delay: FiniteDuration): TaskBuilder = new TaskBuilder(delay)

  /**
   * Performs a task after the specified interval and repeatedly thereafter, waiting the specified interval between
   * invocations.
   *
   * @param interval The amount of time to wait before performing the task and between subsequent executions.
   * @param action The action to repeatedly perform.
   * @param context The execution context to perform the action on.
   */
  def every[T](interval: FiniteDuration)(action: => T)(implicit context: ExecutionContext): Task =
    after(interval).thenEvery(interval)(action)

  /**
   * Represents a task spawned by a timer.
   *
   * @param cancellable The underlying cancellable scheduler task.
   */
  final class Task private[Timer] (cancellable: Cancellable) {

    /** Returns true if this task has been cancelled. */
    def isCancelled: Boolean = cancellable.isCancelled

    /** Cancels this task. */
    def cancel(): Unit = cancellable.cancel()

  }

  /**
   * A generic task builder.
   *
   * @param delay The initial delay duration.
   */
  final class TaskBuilder private[Timer] (val delay: FiniteDuration) extends AnyVal {

    /**
     * Performs a task once after this builder's delay.
     *
     * @param action The action to perform after the specified delay.
     * @param context The execution context to perform the action on.
     */
    def apply[T](action: => T)(implicit context: ExecutionContext): Task =
      new Task(scheduler.scheduleOnce(delay)(action))

    /**
     * Performs a task after this builder's delay and repeatedly thereafter, waiting the specified interval between
     * invocations.
     *
     * @param interval The amount of time to wait between subsequent executions.
     * @param action The action to repeatedly perform.
     * @param context The execution context to perform the action on.
     */
    def thenEvery[T](interval: FiniteDuration)(action: => T)(implicit context: ExecutionContext): Task =
      new Task(scheduler.schedule(delay, interval)(action))

  }

}