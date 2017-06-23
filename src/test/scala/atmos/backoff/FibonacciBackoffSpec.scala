/* FibonacciBackoffSpec.scala
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
 * Test suite for [[atmos.backoff.FibonacciBackoff]].
 */
class FibonacciBackoffSpec extends FlatSpec with Matchers {

  val result = "result"
  val thrown = new RuntimeException

  "FibonacciBackoff" should "scale its backoff by repeatedly multiplying with an approximation of the golden ratio" in {
    for {
      backoff <- 1L to 100L map (100.millis * _)
      policy = FibonacciBackoff(backoff)
      outcome <- Seq(Success(result), Failure(thrown))
      attempt <- 1 to 10
    } policy.nextBackoff(attempt, outcome) shouldEqual scale(backoff.toNanos, attempt).round.nanos
  }

  /** Scales a number by repeatedly multiplying with an approximation of the golden ratio. */
  def scale(n: Double, attempts: Int): Double = if (attempts <= 1) n else scale(n * 8.0 / 5.0, attempts - 1)

}