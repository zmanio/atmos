/* BackoffPolicySpec.scala
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
 * 
 * Portions of this code are derived from https://github.com/aboisvert/pixii
 * and https://github.com/lpryor/squishy.
 */
package atmos.retries

import scala.concurrent.duration._
import org.scalatest._

/**
 * Test suite for [[atmos.retries.BackoffPolicy]].
 */
class BackoffPolicySpec extends FlatSpec with Matchers {

  val backoff = 1.second
  val error = new RuntimeException

  "BackoffPolicy.Constant" should "always return the initial backoff" in {
    val policy = BackoffPolicy.Constant(backoff)
    policy.nextBackoff(1, error) shouldEqual backoff
    policy.nextBackoff(2, error) shouldEqual backoff
    policy.nextBackoff(3, error) shouldEqual backoff
  }

  "BackoffPolicy.Linear" should "increase the backoff by the initial backoff after every attempt" in {
    val policy = BackoffPolicy.Linear(backoff)
    policy.nextBackoff(1, error) shouldEqual backoff
    policy.nextBackoff(2, error) shouldEqual backoff * 2
    policy.nextBackoff(3, error) shouldEqual backoff * 3
  }

  "BackoffPolicy.Exponential" should "increase the backoff by doubling the previous backoff after every attempt" in {
    val policy = BackoffPolicy.Exponential(backoff)
    policy.nextBackoff(1, error) shouldEqual backoff
    policy.nextBackoff(2, error) shouldEqual backoff * 2
    policy.nextBackoff(3, error) shouldEqual backoff * 4
  }

  "BackoffPolicy.Fibonacci" should "multiply the previous backoff by the golden ratio after every attempt" in {
    val policy = BackoffPolicy.Fibonacci(backoff)
    policy.nextBackoff(1, error) shouldEqual backoff
    policy.nextBackoff(2, error) shouldEqual backoff * 8 / 5
    policy.nextBackoff(3, error) shouldEqual backoff * 8 / 5 * 8 / 5
  }

  "BackoffPolicy.Selected" should "select a backoff policy based on the most recent exception" in {
    val one = 1.second
    val ten = 10.seconds
    val policy = BackoffPolicy.Selected {
      case _: TestError => BackoffPolicy.Constant(ten)
      case _ => BackoffPolicy.Linear(one)
    }
    policy.nextBackoff(1, error) shouldEqual one
    policy.nextBackoff(2, new TestError) shouldEqual ten
    policy.nextBackoff(3, error) shouldEqual one * 3
    policy.nextBackoff(4, new TestError) shouldEqual ten
    policy.nextBackoff(5, error) shouldEqual one * 5
  }

  "BackoffPolicy.Randomized" should "incorporate a random number into a backoff duration" in {
    val min = -10.millis
    val max = 10.millis
    val policy = BackoffPolicy.Randomized(BackoffPolicy.Constant(Duration.Zero), min -> max)
    for (attempt <- 1 to 10) {
      val backoff = policy.nextBackoff(attempt, error)
      backoff should be <= max
      backoff should be >= min
    }
  }

  private class TestError extends RuntimeException

}