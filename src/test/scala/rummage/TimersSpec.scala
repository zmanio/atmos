/* TimersSpec.scala
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
import java.util.concurrent.{Callable, ScheduledExecutorService, ScheduledFuture}
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Test suite for the rummage timer API.
 */
class TimersSpec extends FlatSpec with Matchers with MockFactory {

  "Timer" should "provide a fluent interface around task submission" in {
    import ExecutionContext.Implicits._
    val timer = new TimerMock
    // Test `timer after delay`
    timer.submit.expects(1.second, None, *, global).returns(DummyTask).once
    (timer.mock after 1.second) {()} shouldBe DummyTask
    // Test `timer after delay andEvery interval`
    timer.submit.expects(1.second, Some(2.seconds), *, global).returns(DummyTask).once
    (timer.mock after 1.second andEvery 2.seconds) {()} shouldBe DummyTask
    // Test `timer every interval`
    timer.submit.expects(3.seconds, Some(3.seconds), *, global).returns(DummyTask).once
    (timer.mock every 3.seconds) {()} shouldBe DummyTask
  }

  /** A container for the timer mocking utilities, used so we only mock the submit method. */
  class TimerMock {
    self =>
    val submit = mockFunction[FiniteDuration, Option[FiniteDuration], () => Unit, ExecutionContext, Timer.Task]
    val mock = new Timer {
      def submit(delay: FiniteDuration, interval: Option[FiniteDuration], f: () => Unit)(
        implicit ec: ExecutionContext) = self.submit(delay, interval, f, ec)
    }
  }

  /** A placeholder for a task. */
  object DummyTask extends Timer.Task {
    def isCancelled = true

    def cancel() = {}
  }

  "JavaTimer" should "schedule tasks using a scheduled executor service" in {
    import ExecutionContext.Implicits._
    val executor = new ExecutorMock
    val timer = JavaTimer(executor.mock)
    val future = mock[ScheduledFuture[AnyRef]]
    // Test `timer after delay`
    executor.schedule.expects(*, 1.second.toMillis, MILLISECONDS).returns(future).once
    val task1 = (timer after 1.second) {()}
    (future.isCancelled _).expects().returns(false).once
    task1.isCancelled shouldBe false
    (future.cancel _).expects(false).returns(true).once
    task1.cancel()
    // Test `timer after delay andEvery interval`
    executor.scheduleAtFixedRate.expects(*, 1.second.toMillis, 2.seconds.toMillis, MILLISECONDS).returns(future).once
    val task2 = (timer after 1.second andEvery 2.seconds) {()}
    (future.isCancelled _).expects().returns(false).once
    task2.isCancelled shouldBe false
    (future.cancel _).expects(false).returns(true).once
    task2.cancel()
    // Test `timer every interval`
    executor.scheduleAtFixedRate.expects(*, 3.seconds.toMillis, 3.seconds.toMillis, MILLISECONDS).returns(future).once
    val task3 = (timer every 3.seconds) {()}
    (future.isCancelled _).expects().returns(false).once
    task3.isCancelled shouldBe false
    (future.cancel _).expects(false).returns(true).once
    task3.cancel()
  }

  /** A container for the scheduled executor mocking utilities, used because ScalaMock does not like this class. */
  class ExecutorMock {
    self =>
    val schedule = mockFunction[Runnable, Long, TimeUnit, ScheduledFuture[_]]
    val scheduleAtFixedRate = mockFunction[Runnable, Long, Long, TimeUnit, ScheduledFuture[_]]
    val mock = new ScheduledExecutorService {
      def execute(a: Runnable) = ???

      def awaitTermination(a: Long, b: TimeUnit) = ???

      def invokeAll[T](a: java.util.Collection[_ <: Callable[T]], b: Long, c: TimeUnit) = ???

      def invokeAll[T](a: java.util.Collection[_ <: Callable[T]]) = ???

      def invokeAny[T](a: java.util.Collection[_ <: Callable[T]], b: Long, c: TimeUnit) = ???

      def invokeAny[T](a: java.util.Collection[_ <: Callable[T]]) = ???

      def isShutdown() = ???

      def isTerminated() = ???

      def shutdown() = ???

      def shutdownNow() = ???

      def submit(a: Runnable) = ???

      def submit[T](a: Runnable, b: T) = ???

      def submit[T](a: Callable[T]) = ???

      def schedule(a: Runnable, b: Long, c: TimeUnit) = self.schedule(a, b, c)

      def schedule[V](a: Callable[V], b: Long, c: TimeUnit) = ???

      def scheduleAtFixedRate(a: Runnable, b: Long, c: Long, d: TimeUnit) = self.scheduleAtFixedRate(a, b, c, d)

      def scheduleWithFixedDelay(a: Runnable, b: Long, c: Long, d: TimeUnit) = ???
    }
  }

  "AkkaTimer" should "schedule tasks with an Akka scheduler" in {
    import ExecutionContext.Implicits._
    val system = mock[ActorSystem]
    val scheduler = new SchedulerMock
    (system.scheduler _).expects().returning(scheduler.mock).once
    val timer = AkkaTimer(system)
    val cancellable = mock[Cancellable]
    // Test `timer after delay`
    scheduler.scheduleOnce.expects(1.second, *, global).returns(cancellable).once
    val task1 = (timer after 1.second) {()}
    (cancellable.isCancelled _).expects().returns(false).once
    task1.isCancelled shouldBe false
    (cancellable.cancel _).expects().returns(true).once
    task1.cancel()
    // Test `timer after delay andEvery interval`
    scheduler.schedule.expects(1.second, 2.seconds, *, global).returns(cancellable).once
    val task2 = (timer after 1.second andEvery 2.seconds) {()}
    (cancellable.isCancelled _).expects().returns(false).once
    task2.isCancelled shouldBe false
    (cancellable.cancel _).expects().returns(true).once
    task2.cancel()
    // Test `timer every interval`
    scheduler.schedule.expects(3.seconds, 3.seconds, *, global).returns(cancellable).once
    val task3 = (timer every 3.seconds) {()}
    (cancellable.isCancelled _).expects().returns(false).once
    task3.isCancelled shouldBe false
    (cancellable.cancel _).expects().returns(true).once
    task3.cancel()
  }

  /** A container for the scheduler mocking utilities, used because ScalaMock does not like this class. */
  class SchedulerMock {
    self =>
    val scheduleOnce = mockFunction[FiniteDuration, Runnable, ExecutionContext, Cancellable]
    val schedule = mockFunction[FiniteDuration, FiniteDuration, Runnable, ExecutionContext, Cancellable]
    val mock = new Scheduler {
      def maxFrequency: Double = ???

      def scheduleOnce(a: FiniteDuration, b: Runnable)(implicit c: ExecutionContext) = self.scheduleOnce(a, b, c)

      def schedule(a: FiniteDuration, b: FiniteDuration, c: Runnable)(implicit d: ExecutionContext) =
        self.schedule(a, b, c, d)
    }
  }

}