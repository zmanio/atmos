/* TerminationPolicySpec.scala
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
 * Test suite for [[atmos.retries.TerminationPolicy]].
 */
class TerminationPolicySpec extends FlatSpec with Matchers {

  import TerminationPolicy._

  val attempts = 6
  val timeout = 6.seconds

  "TerminationPolicy.ImmediatelyTerminate" should "immediately signal for termination" in {
    val policy = ImmediatelyTerminate
    policy.shouldTerminate(attempts / 2, timeout / 2) shouldEqual true
    policy.shouldTerminate(attempts, timeout) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual true
  }

  "TerminationPolicy.NeverTerminate" should "never signal for termination" in {
    val policy = NeverTerminate
    policy.shouldTerminate(attempts / 2, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts, timeout) shouldEqual false
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual false
  }

  "TerminationPolicy.LimitNumberOfAttempts" should "terminate after a specific number of attempts" in {
    val policy = LimitNumberOfAttempts(attempts)
    policy.shouldTerminate(attempts / 2, timeout * 2) shouldEqual false
    policy.shouldTerminate(attempts, timeout * 2) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual true
  }

  "TerminationPolicy.LimitAmountOfTimeSpent" should "terminate after a specific duration" in {
    val policy = LimitAmountOfTimeSpent(timeout)
    policy.shouldTerminate(attempts * 2, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts * 2, timeout) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual true
  }

  "TerminationPolicy.TerminateAfterBoth" should "terminate only when both the specified policies terminate" in {
    val policy = TerminateAfterBoth(LimitNumberOfAttempts(attempts), LimitAmountOfTimeSpent(timeout))
    policy.shouldTerminate(attempts / 2, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts / 2, timeout) shouldEqual false
    policy.shouldTerminate(attempts / 2, timeout * 2) shouldEqual false
    policy.shouldTerminate(attempts, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts, timeout) shouldEqual true
    policy.shouldTerminate(attempts, timeout * 2) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts * 2, timeout) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual true
  }

  "TerminationPolicy.TerminateAfterEither" should "terminate when either of the specified policies terminate" in {
    val policy = TerminateAfterEither(LimitNumberOfAttempts(attempts), LimitAmountOfTimeSpent(timeout))
    policy.shouldTerminate(attempts / 2, timeout / 2) shouldEqual false
    policy.shouldTerminate(attempts / 2, timeout) shouldEqual true
    policy.shouldTerminate(attempts / 2, timeout * 2) shouldEqual true
    policy.shouldTerminate(attempts, timeout / 2) shouldEqual true
    policy.shouldTerminate(attempts, timeout) shouldEqual true
    policy.shouldTerminate(attempts, timeout * 2) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout / 2) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout) shouldEqual true
    policy.shouldTerminate(attempts * 2, timeout * 2) shouldEqual true
  }

}