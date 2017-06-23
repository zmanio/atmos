/* timers.scala
 * 
 * Copyright (c) 2013-2015 linkedin.com
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
package rummage

import akka.actor.{ActorSystem, Cancellable, Scheduler}
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * An interface for scheduling tasks to run one or more times at some point in the future.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
trait Timer {

  import Timer.Task

  /**
   * Creates a builder capable of building timers that perform a task once or repeatedly after the specified delay.
   *
   * @param delay The amount of time to wait before performing the task for the first time.
   */
  def after(delay: FiniteDuration): Builder = new Builder(delay)

  /**
   * Performs a task after the specified delay and repeatedly thereafter, waiting the specified interval between
   * invocations.
   *
   * @tparam U Any type, treated as if it was `Unit`.
   * @param interval The amount of time to wait before performing the task and between subsequent executions.
   * @param f        The action to repeatedly perform.
   * @param ec       The execution context to perform the action on.
   */
  def every[U](interval: FiniteDuration)(f: => U)(implicit ec: ExecutionContext): Task =
    submit(interval, Some(interval), f _)

  /**
   * Submits a task for execution after the specified delay and possibly repeatedly thereafter, waiting the specified
   * interval (if specified) between invocations.
   *
   * @param delay    The amount of time to wait before performing the task for the first time.
   * @param interval A `Some` containing the amount of time to wait between subsequent executions or `None`.
   * @param f        The action to (possibly repeatedly) perform.
   * @param ec       The execution context to perform the action on.
   */
  def submit(delay: FiniteDuration, interval: Option[FiniteDuration], f: () => Unit)(
    implicit ec: ExecutionContext): Task

  /**
   * A secondary interface for building tasks that fire after an initial delay.
   *
   * @param delay The initial delay duration.
   */
  final class Builder private[Timer](delay: FiniteDuration) {

    /**
     * Performs a task once after this builder's delay.
     *
     * @tparam U Any type, treated as if it was `Unit`.
     * @param f  The action to perform after the specified delay.
     * @param ec The execution context to perform the action on.
     */
    def apply[U](f: => U)(implicit ec: ExecutionContext): Task =
      submit(delay, None, f _)

    /**
     * Performs a task once after this builder's delay and repeatedly thereafter, waiting the specified interval
     * between subsequent invocations.
     *
     * @tparam U Any type, treated as if it was `Unit`.
     * @param interval The amount of time to wait between subsequent executions.
     * @param f        The action to repeatedly perform.
     * @param ec       The execution context to perform the action on.
     */
    def andEvery[U](interval: FiniteDuration)(f: => U)(implicit ec: ExecutionContext): Task =
      submit(delay, Some(interval), f _)

  }

}

/**
 * Definitions associated with timers as well as the infrastructure for publishing the global implicit timer.
 *
 * This object publishes an implicit, lazily-initialized, global timer backed by a single daemon thread. It
 * additionally implements `Timer` itself and forwards all task submissions to the aforementioned global timer.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
object Timer extends Timer {

  /** The default global timer backed by a single thread. */
  implicit lazy val global: Timer = new JavaTimer(Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
    val counter = new AtomicLong

    def newThread(r: Runnable) = {
      val thread = new Thread(r, s"${classOf[Timer].getName}-${counter.incrementAndGet()}")
      thread.setDaemon(true)
      thread
    }
  }))

  /* Delegates to the global timer instance. */
  def submit(delay: FiniteDuration, interval: Option[FiniteDuration], f: () => Unit)(implicit ec: ExecutionContext) =
    global.submit(delay, interval, f)

  /**
   * Represents a task spawned by a timer.
   */
  trait Task {

    /** Returns true if this task has been cancelled. */
    def isCancelled: Boolean

    /** Cancels this task. */
    def cancel(): Unit

  }

}

/**
 * A timer implemented with a Java `ScheduledExecutorService`.
 *
 * @param executor The `ScheduledExecutorService` instance to view as a timer.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
final class JavaTimer(executor: ScheduledExecutorService) extends Timer {
  def submit(delay: FiniteDuration, interval: Option[FiniteDuration], f: () => Unit)(implicit ec: ExecutionContext) = {
    @volatile var handle = None: Option[ScheduledFuture[_]]
    val nested = new Runnable {def run = if (handle forall (!_.isCancelled)) f()}
    val action = new Runnable {def run = ec execute nested}
    val future = interval match {
      case Some(interval) => executor.scheduleAtFixedRate(action, delay.toMillis, interval.toMillis, MILLISECONDS)
      case None => executor.schedule(action, delay.toMillis, MILLISECONDS)
    }
    handle = Some(future)
    new Timer.Task {
      def isCancelled = future.isCancelled

      def cancel() = future.cancel(false)
    }: Timer.Task
  }
}

/**
 * Factory for timers implemented with a Java `ScheduledExecutorService`.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
object JavaTimer {

  /**
   * Creates a Java timer from the specified executor.
   *
   * @param executor The `ScheduledExecutorService` instance to view as a timer.
   */
  def apply(executor: ScheduledExecutorService): JavaTimer = new JavaTimer(executor)

}

/**
 * A timer implemented with an Akka `Scheduler`.
 *
 * @param scheduler The `Scheduler` instance to view as a timer.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
final class AkkaTimer(scheduler: Scheduler) extends Timer {
  def submit(delay: FiniteDuration, interval: Option[FiniteDuration], f: () => Unit)(implicit ec: ExecutionContext) = {
    @volatile var handle = None: Option[Cancellable]
    val action = new Runnable {def run = if (handle forall (!_.isCancelled)) f()}
    val cancellable = interval match {
      case Some(interval) => scheduler.schedule(delay, interval, action)
      case None => scheduler.scheduleOnce(delay, action)
    }
    handle = Some(cancellable)
    new Timer.Task {
      def isCancelled = cancellable.isCancelled

      def cancel() = cancellable.cancel()
    }: Timer.Task
  }
}

/**
 * Factory for timers implemented with an Akka `Scheduler`.
 */
@deprecated("Timers are replaced by the Clock API", "1.1")
object AkkaTimer {

  /**
   * Creates an Akka timer from the specified scheduler.
   *
   * @param scheduler The `Scheduler` instance to view as a timer.
   */
  def apply(scheduler: Scheduler): AkkaTimer = new AkkaTimer(scheduler)

  /**
   * Creates an Akka timer from the specified actor system's scheduler.
   *
   * @param system The actor system that contains the scheduler to view as a timer.
   */
  def apply(system: ActorSystem): AkkaTimer = AkkaTimer(system.scheduler)

}