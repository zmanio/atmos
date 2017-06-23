/* deadlines.scala
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
package rummage

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

/**
 * A factory for deadline-bound futures.
 */
object Deadline {

  /**
   * Returns a future that completes with the result of `future` when it completes or with a `DeadlineException` after
   * `duration` elapses, whichever comes first.
   *
   * @tparam T The underlying type of the resulting future.
   * @param duration The time limit that the supplied future must complete in.
   * @param future   The future that is being bound to a deadline.
   * @param ec       The execution context to perform asynchronous operations on.
   * @param clock    The clock to use for scheduling the deadline notification.
   */
  def apply[T](duration: FiniteDuration)(future: Future[T])(implicit ec: ExecutionContext, clock: Clock): Future[T] =
    Future firstCompletedOf Vector(
      future,
      clock asyncWait duration flatMap { _ => Future failed DeadlineException(duration) })

}

/**
 * The exception returned when a deadline cannot be met.
 *
 * @param duration The time limit that was exceeded.
 */
case class DeadlineException(duration: FiniteDuration)
  extends RuntimeException(s"Deadline of $duration was exceeded.") with NoStackTrace

/**
 * Definition of the deadline DSL.
 */
trait Deadlines {

  /**
   * Extends the `Future` type with the ability to apply a deadline.
   *
   * @tparam T The underlying type of the wrapped future.
   * @param self The future that is being bound to a deadline.
   */
  implicit class DeadlineSupport[T](val self: Future[T]) {

    /**
     * Returns a future that completes with the result of `self` when it completes or with a `DeadlineException` after
     * `duration` elapses, whichever comes first.
     *
     * @param duration The time limit that the supplied future must complete in.
     * @param ec       The execution context to perform asynchronous operations on.
     * @param clock    The clock to use for scheduling the deadline notification.
     */
    def withDeadline(duration: FiniteDuration)(implicit ec: ExecutionContext, clock: Clock): Future[T] =
      Deadline(duration)(self)

  }

}

/**
 * Global implementation of the deadline DSL.
 */
object Deadlines extends Deadlines
