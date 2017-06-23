/* RequireEitherSpec.scala
 * 
 * Copyright (c) 2013-2014 linkedin.com
 * Copyright (c) 2013-2014 zman.io
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
package atmos.termination

import org.scalatest._
import scala.concurrent.duration._

/**
 * Test suite for [[atmos.termination.RequireEither]].
 */
class RequireEitherSpec extends FlatSpec with Matchers {

  "RequireEither" should "signal for termination as soon as either of its child policies do" in {
    val policies = for {
      first <- Seq(AlwaysTerminate, NeverTerminate)
      second <- Seq(AlwaysTerminate, NeverTerminate)
    } yield RequireEither(first, second) -> (first == AlwaysTerminate || second == AlwaysTerminate)
    for {
      (policy, expectedResult) <- policies
      nextAttemptAt <- 1L to 100L map (100.millis * _)
      attempt <- 1 to 10
    } policy.shouldTerminate(attempt, nextAttemptAt) shouldEqual expectedResult
  }

}