/* ClocksSpec.scala
 * 
 * Copyright (c) 2014-2015 linkedin.com
 * Copyright (c) 2014-2015 zman.io
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

import akka.actor.{ActorContext, ActorRef, ActorSystem, Cancellable, Scheduler}
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

/**
 * Test suite for the rummage clock API.
 */
class ClocksSpec extends FlatSpec with Matchers with MockFactory {

  import ExecutionContext.Implicits._

  val timeTolerance = 20 millis
  val syncWaitTolerance = 40 millis
  val asyncWaitTolerance = 60 millis

  "The default clock" should "wrap system time primitives" in {
    val clock = Clock()
    // Verify that the default clock reports system time.
    clock.now.toMillis shouldBe System.currentTimeMillis +- timeTolerance.toMillis
    clock.tick.toNanos shouldBe System.nanoTime +- timeTolerance.toNanos
    // Verify that the clock correctly synchronously waits.
    timed(clock.syncWait(500 millis), syncWaitTolerance).toMillis shouldBe 500L +- syncWaitTolerance.toMillis
    // Verify that the clock correctly asynchronously waits.
    timed(Await.result(clock.asyncWait(1 second), Duration.Inf), asyncWaitTolerance).toMillis shouldBe
    1000L +- asyncWaitTolerance.toMillis
  }

  "The Akka clock" should "use an Akka scheduler for asynchronous waiting" in {
    val scheduler = new SchedulerMock
    val clock = AkkaClock(scheduler.mock)
    // Verify that the clock correctly asynchronously waits.
    scheduler.scheduleOnce.expects(*, *, global).atLeastOnce.onCall {
      (f: FiniteDuration, r: Runnable, _: ExecutionContext) => Thread.sleep(f toMillis); r.run(); null
    }
    Await.result(clock.asyncWait(1 second), Duration.Inf)
  }

  it should "be available whenever an implicit actor context is available" in {
    val context = new ActorContextMock
    val system = mock[ActorSystem]
    val schedulerMock = new SchedulerMock
    context.system.expects().once.returns(system)
    (system.scheduler _).expects().once.returns(schedulerMock.mock)
    import AkkaClocks._
    implicit val ctx = context.mock
    Clock() shouldBe AkkaClock(schedulerMock.mock)
  }

  "The scaled clock" should "correctly speed up time" in {
    implicit val clock = mock[Clock]
    (clock.now _).expects().once.returns(100 millis)
    (clock.tick _).expects().once.returns(0 nanos)
    val scaled = ScaledClock(1.minute -> 2.minutes)
    // Verify that the clock correctly reports scaled time.
    (clock.tick _).expects().twice.returns(100 millis)
    scaled.now shouldBe 300.millis
    scaled.tick shouldBe 200.millis
    // Verify that the clock correctly synchronously waits.
    (clock.syncWait _).expects(100 millis).once.returns(100 millis)
    scaled.syncWait(200 millis) shouldBe 200.millis
    // Verify that the clock correctly asynchronously waits.
    (clock.asyncWait(_: FiniteDuration)(_: ExecutionContext))
      .expects(100 millis, global).once.returns(Future.successful(100 millis))
    Await.result(scaled.asyncWait(200 millis), Duration.Inf) shouldBe 200.millis
  }

  it should "correctly slow down time" in {
    implicit val clock = mock[Clock]
    (clock.now _).expects().once.returns(100 millis)
    (clock.tick _).expects().once.returns(0 nanos)
    val scaled = ScaledClock(2.minutes -> 1.minute)
    // Verify that the clock correctly reports scaled time.
    (clock.tick _).expects().twice.returns(100 millis)
    scaled.now shouldBe 150.millis
    scaled.tick shouldBe 50.millis
    // Verify that the clock correctly synchronously waits.
    (clock.syncWait _).expects(100 millis).once.returns(100 millis)
    scaled.syncWait(50 millis) shouldBe 50.millis
    // Verify that the clock correctly asynchronously waits.
    (clock.asyncWait(_: FiniteDuration)(_: ExecutionContext))
      .expects(100 millis, global).once.returns(Future.successful(100 millis))
    Await.result(scaled.asyncWait(50 millis), Duration.Inf) shouldBe 50.millis
  }

  it should "reject invalid scaling factors" in {
    an[IllegalArgumentException] should be thrownBy ScaledClock(0.0)
    an[IllegalArgumentException] should be thrownBy ScaledClock(-1.0)
    an[IllegalArgumentException] should be thrownBy ScaledClock(Double.NaN)
    an[IllegalArgumentException] should be thrownBy ScaledClock(Double.PositiveInfinity)
    an[IllegalArgumentException] should be thrownBy ScaledClock(Double.NegativeInfinity)
    an[IllegalArgumentException] should be thrownBy ScaledClock(1.second -> 0.seconds)
    an[IllegalArgumentException] should be thrownBy ScaledClock(0.seconds -> 1.second)
  }

  /** A utility that times a function and verifies that it reports the correct duration. */
  def timed(f: => FiniteDuration, tolerance: FiniteDuration): FiniteDuration = {
    val start = System.nanoTime
    val waited = f
    val end = System.nanoTime
    waited.toNanos shouldBe (end - start) +- tolerance.toNanos
    waited
  }

  /** A container for the Akka scheduler mocking utilities, used because ScalaMock does not like this class. */
  class SchedulerMock {
    outer =>
    val scheduleOnce = mockFunction[FiniteDuration, Runnable, ExecutionContext, Cancellable]
    val mock = new Scheduler {
      def maxFrequency: Double = ???

      def scheduleOnce(a: FiniteDuration, b: Runnable)(implicit c: ExecutionContext) = outer.scheduleOnce(a, b, c)

      def schedule(a: FiniteDuration, b: FiniteDuration, c: Runnable)(implicit d: ExecutionContext) = ???
    }
  }

  /** A container for the Akka actor context mocking utilities, used because ScalaMock does not like this class. */
  class ActorContextMock {
    outer =>
    val system = mockFunction[ActorSystem]
    val mock = new ActorContext {
      override def watchWith(subject: ActorRef, msg: Any): ActorRef = ???

      def system = outer.system()

      def become(behavior: akka.actor.Actor.Receive, discardOld: Boolean) = ???

      def child(name: String) = ???

      def children = ???

      def dispatcher = ???

      def parent = ???

      def props = ???

      def receiveTimeout = ???

      def self = ???

      def sender() = ???

      def setReceiveTimeout(timeout: scala.concurrent.duration.Duration) = ???

      def unbecome() = ???

      def unwatch(subject: akka.actor.ActorRef) = ???

      def watch(subject: akka.actor.ActorRef) = ???

      def actorOf(props: akka.actor.Props, name: String) = ???

      def actorOf(props: akka.actor.Props) = ???

      def stop(actor: akka.actor.ActorRef) = ???

      def guardian = ???

      def lookupRoot = ???

      def provider = ???

      def systemImpl = ???
    }
  }

}