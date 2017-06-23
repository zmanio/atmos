/* ExponentialBackoffSpec.scala
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
 * Test suite for [[atmos.backoff.ExponentialBackoff]].
 */
class ExponentialBackoffSpec extends FlatSpec with Matchers {

  val result = "result"
  val thrown = new RuntimeException

  "ExponentialBackoff" should "scale its backoff by repeatedly multiplying by two" in {
    for {
      backoff <- 1L to 100L map (100.millis * _)
      policy = ExponentialBackoff(backoff)
      outcome <- Seq(Success(result), Failure(thrown))
      attempt <- 1 to 10
    } policy.nextBackoff(attempt, outcome) shouldEqual scale(backoff.toNanos, attempt).nanos
  }

  /** Scales a number by repeatedly multiplying by two. */
  def scale(n: Long, attempts: Int): Long = if (attempts <= 1) n else scale(n * 2, attempts - 1)

}