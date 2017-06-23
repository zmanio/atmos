/* NeverTerminateSpec.scala
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
 * Test suite for [[atmos.termination.NeverTerminate]].
 */
class NeverTerminateSpec extends FlatSpec with Matchers {

  "NeverTerminate" should "not signal for termination on any combination of inputs" in {
    for {
      nextAttemptAt <- 1L to 100L map (100.millis * _)
      attempt <- 1 to 10
    } NeverTerminate.shouldTerminate(attempt, nextAttemptAt) shouldEqual false
  }

}