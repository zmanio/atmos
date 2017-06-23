/* SelectedBackoffSpec.scala
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
 * Test suite for [[atmos.backoff.SelectedBackoff]].
 */
class SelectedBackoffSpec extends FlatSpec with Matchers {

  val normalResult = "normal"
  val specialResult = "special"
  val normalThrown = new RuntimeException
  val specialThrown = new RuntimeException

  "SelectedBackoff" should "select another backoff policy to used based on the most recent outcome" in {
    for {
      backoff <- 1L to 100L map (100.millis * _)
      rNormalPolicy = ConstantBackoff(backoff)
      rSpecialPolicy = LinearBackoff(backoff)
      tNormalPolicy = ExponentialBackoff(backoff)
      tSpecialPolicy = FibonacciBackoff(backoff)
      policy = SelectedBackoff {
        case Success(r) if r == normalResult => rNormalPolicy
        case Success(r) if r == specialResult => rSpecialPolicy
        case Failure(t) if t == normalThrown => tNormalPolicy
        case Failure(t) if t == specialThrown => tSpecialPolicy
      }
      (outcome, expectedPolicy) <- Seq(
        Success(normalResult) -> rNormalPolicy,
        Success(specialResult) -> rSpecialPolicy,
        Failure(normalThrown) -> tNormalPolicy,
        Failure(specialThrown) -> tSpecialPolicy)
      attempt <- 1 to 10
    } policy.nextBackoff(attempt, outcome) shouldEqual expectedPolicy.nextBackoff(attempt, outcome)
  }

}