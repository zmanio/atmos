/* RandomizedBackoffSpec.scala
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
package atmos.backoff

import org.scalatest._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Test suite for [[atmos.backoff.RandomizedBackoff]].
 */
class RandomizedBackoffSpec extends FlatSpec with Matchers {

  val result = "result"
  val thrown = new RuntimeException

  "RandomizedBackoff" should "adjust the result of another backoff policy with a random value" in {
    for {
      backoff <- 1L to 100L map (100.millis * _)
      (first, second) <- Seq(-10.millis -> 10.millis, 0.millis -> 0.millis, 10.millis -> 0.millis)
      policy = RandomizedBackoff(ConstantBackoff(backoff), first -> second)
      outcome <- Seq(Success(result), Failure(thrown))
      attempt <- 1 to 10
    } checkBackoff(backoff, first, second, policy.nextBackoff(attempt, outcome))
  }

  /** Checks that a randomized duration conforms to the expected range. */
  def checkBackoff(base: FiniteDuration, first: FiniteDuration, second: FiniteDuration, result: FiniteDuration) =
    if (first == second) result shouldEqual base
    else {
      val b = base.toNanos
      val f = first.toNanos
      val s = second.toNanos
      val min = b + math.min(f, s)
      val max = b + math.max(f, s)
      val half = (max - min) / 2
      result.toNanos shouldEqual (min + half) +- half
    }

}