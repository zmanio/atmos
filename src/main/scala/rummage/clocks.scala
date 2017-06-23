/* clocks.scala
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

import akka.actor.{ActorContext, Scheduler}
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{Failure, Success, Try}

/**
 * A low-level abstraction over time. This class supports querying the absolute time and the monitoring flow of time as
 * well as waiting for future times both synchronously and asynchronously.
 */
trait Clock {

  /** Returns the amount of time that has passed since 12:00 am January 1 1970 GMT. */
  def now: FiniteDuration

  /**
   * Returns the amount of time that has passed since an arbitrary point in history. The results of this method are
   * only useful when compared against the result of other invocations of this method.
   */
  def tick: FiniteDuration

  /**
   * Attempts to wait for the specified duration, completing after it has elapsed or if an error is encountered.
   *
   * @param timeout The amount of time to block the calling thread.
   */
  def syncWait(timeout: FiniteDuration): FiniteDuration

  /**
   * Returns a future that completes after the specified duration has elapsed or if an error is encountered.
   *
   * @param timeout The amount of time that must pass before the returned future completes.
   * @param ec      The execution context to perform wait operations with.
   */
  def asyncWait(timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[FiniteDuration]

}

/** Primary factory and resolver for instances of `Clock`. */
object Clock {

  /** Returns the implicit clock from the caller's context. */
  def apply()(implicit clock: Clock): Clock = clock

  /** An implementation of the query methods in `Clock` using system time. */
  trait SystemTime extends Clock {

    /* Return the time from `System.currentTimeMillis`. */
    override def now = System.currentTimeMillis.millis

    /* Return the tick from `System.nanoTime`. */
    override def tick = System.nanoTime.nanos

  }

  /** An implementation of the synchronous waiting in `Clock` using `Thread.sleep()`. */
  trait SyncWaitBySleeping extends Clock {

    /* Use `Thread.sleep()` until the specified timeout has elapsed. */
    override def syncWait(timeout: FiniteDuration) = {
      val waitUntil = tick + timeout

      @annotation.tailrec
      def waiting(): FiniteDuration = {
        val remaining = waitUntil - tick
        if (remaining <= Duration.Zero) {
          timeout + -remaining
        } else {
          blocking {remaining.unit.sleep(remaining.length)}
          waiting()
        }
      }

      waiting()
    }

  }

  /** An implementation of the asynchronous waiting in `Clock` using a `ScheduledExecutorService`. */
  trait AsyncWaitWithScheduledExecutor extends Clock {

    /** The scheduled executor to use. */
    protected def scheduledExecutor: ScheduledExecutorService

    /* Use `ScheduledExecutorService.schedule()` to schedule tasks to fulfill a promise after the timeout elapses. */
    override def asyncWait(timeout: FiniteDuration)(implicit ec: ExecutionContext) = {
      val waitUntil = tick + timeout
      val promise = Promise[FiniteDuration]()
      new Runnable {
        outer =>
        val inner = new Runnable {
          override def run = {
            val remaining = waitUntil - tick
            if (remaining <= Duration.Zero) {
              promise.complete(Success(timeout + -remaining))
            } else {
              Try {scheduledExecutor.schedule(outer, remaining.length, remaining.unit)} match {
                case Failure(e) => promise.complete(Failure(e))
                case _ =>
              }
            }
          }
        }

        override def run = ec execute inner
      }.inner.run()
      promise.future
    }

  }

  /**
   * The default clock based on system time, using `Thread.sleep()` for synchronous waiting and a global,
   * single-threaded `ScheduledExecutorService` for asynchronous waiting.
   */
  implicit object Default extends SystemTime with SyncWaitBySleeping with AsyncWaitWithScheduledExecutor {

    /* A global, single-threaded `ScheduledExecutor` to enqueue asynchronous tasks with. */
    override protected val scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
      val counter = new AtomicLong

      def newThread(r: Runnable) = {
        val thread = new Thread(r, s"${classOf[Clock].getName}-${counter.incrementAndGet()}")
        thread.setDaemon(true)
        thread
      }
    })

  }

}

/**
 *
 * A clock for use in Akka actors, using system time and Thread.sleep()` for synchronous waiting, but using an Akka
 * `Scheduler` for asynchronous waiting.
 *
 * @param akkaScheduler The Akka scheduler to use.
 */
case class AkkaClock(akkaScheduler: Scheduler)
  extends Clock.SystemTime with Clock.SyncWaitBySleeping with AkkaClock.AsyncWaitWithAkkaScheduler

/** Support for the `AkkaClock` class. */
object AkkaClock {

  /** An implementation of the asynchronous waiting in `Clock` using an Akka `Scheduler`. */
  trait AsyncWaitWithAkkaScheduler extends Clock {

    /** The Akka scheduler to use. */
    def akkaScheduler: Scheduler

    /* Use `Scheduler.scheduleOnce()` to schedule tasks to fulfill a promise after the timeout elapses. */
    override def asyncWait(timeout: FiniteDuration)(implicit ec: ExecutionContext) = {
      val waitUntil = tick + timeout
      val promise = Promise[FiniteDuration]()
      new Runnable {
        override def run = {
          val remaining = waitUntil - tick
          if (remaining <= Duration.Zero) {
            promise.complete(Success(timeout + -remaining))
          } else {
            Try {akkaScheduler.scheduleOnce(remaining, this)} match {
              case Failure(e) => promise.complete(Failure(e))
              case _ =>
            }
          }
        }
      }.run()
      promise.future
    }

  }

}

/** A trait that defines when an implicit Akka clock is available. */
trait AkkaClocks {

  /** An implicit Akka clock is available wherever an actor context is available. */
  implicit def implicitAkkaClock(implicit ctx: ActorContext): Clock =
    AkkaClock(ctx.system.scheduler)

}

/** Global access to the implicit Akka clock publisher. */
object AkkaClocks extends AkkaClocks

/**
 * A clock that scales the progression of time reported by another clock.
 *
 * The provided `factor` maps time from the underlying clock into time for the scaled clock. For example: a scale of
 * `2.0` will result in the scaled clock reporting the passage of time twice as quickly as the underlying clock, since
 * every second of real time equates to two seconds of scaled time. Conversely, a scale of `0.5` will result in the
 * scaled clock reporting the passage of time half as quickly as the underlying clock.
 *
 * @param factor The scaling factor to apply to the passage of time.
 * @param clock  The underlying clock to scale the time of.
 */
case class ScaledClock(factor: Double)(implicit clock: Clock) extends Clock {

  // Factor must be a positive, non-infinite real number.
  if (factor <= 0.0 || factor.isInfinity || factor.isNaN) {
    throw new IllegalArgumentException(s"Invalid scaling factor $factor")
  }

  /** The baseline to report absolute time from. */
  private val start = clock.now

  /** The offset in the underlying tick to proceed from. */
  private val offset = clock.tick

  /* Calculate time by adding the scaled tick duration to the absolute time baseline. */
  override def now = start + tick

  /* Scale the passage of time in the underlying clock since the `offset` was captured. */
  override def tick = scaleHistoricalTime(clock.tick - offset)

  /* Wait by scaling both the timeout and the resulting report of the amount of time waited. */
  override def syncWait(timeout: FiniteDuration) =
    scaleHistoricalTime(clock.syncWait(scaleFutureTime(timeout)))

  /* Wait by scaling both the timeout and the resulting report of the amount of time waited. */
  override def asyncWait(timeout: FiniteDuration)(implicit ec: ExecutionContext) =
    clock.asyncWait(scaleFutureTime(timeout)) map scaleHistoricalTime

  /**
   * Scale a duration that transpired in the past. For example, if the scaled clock is running twice as fast as its
   * underlying clock, then half as much historical time will actually pass.
   */
  private def scaleHistoricalTime(duration: FiniteDuration): FiniteDuration = toFiniteDuration(duration * factor)

  /**
   * Scale a duration that is to transpire in the future. For example, if the scaled clock is running twice as fast as
   * its underlying clock, then half as much future time will actually pass.
   */
  private def scaleFutureTime(duration: FiniteDuration): FiniteDuration = toFiniteDuration(duration / factor)

  /** Converts a duration to a finite duration or fails if it is not possible. */
  private def toFiniteDuration(duration: Duration) = duration match {
    case duration: FiniteDuration => duration
    case duration => throw new IllegalArgumentException(s"Unable to use scaled duration $duration")
  }

}

/** A factory for scaled clocks. */
object ScaledClock {

  /**
   * Creates a scaled clock by mapping the passage of time in the underlying clock to the passage of time in the scaled
   * clock.
   *
   * For example: `ScaledClock(10.seconds -> 10.minutes)` will create a clock that reports the passing of ten minutes
   * for every 10 seconds that pass according to the underlying clock.
   *
   * @param scale The ratio of time from the underlying clock to time in the scaled clock.
   * @param clock The underlying clock to scale the time of.
   */
  def apply(scale: (FiniteDuration, FiniteDuration))(implicit clock: Clock): ScaledClock = {
    val (source, destination) = scale
    if (source <= Duration.Zero) {
      throw new IllegalArgumentException(s"Invalid source scaling factor $source")
    } else if (destination <= Duration.Zero) {
      throw new IllegalArgumentException(s"Invalid destination scaling factor $destination")
    }
    ScaledClock(destination.toNanos / source.toNanos.toDouble)
  }

}