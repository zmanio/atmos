/* IgnoreEventsSpec.scala
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
package atmos.monitor

import org.scalatest._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Test suite for [[atmos.monitor.IgnoreEvents]].
 */
class IgnoreEventsSpec extends FlatSpec with Matchers {

  val result = "result"
  val thrown = new RuntimeException

  "IgnoreEvents" should "do nothing in response to retry events" in {
    // Since it's impossible to prove a negative, this test only calls the target class with no expectations.
    for {
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 100L map {100.millis * _}
        silent <- Seq(true, false)
      } IgnoreEvents.retrying(name, outcome, attempt, backoff, silent)
      IgnoreEvents.interrupted(name, outcome, attempt)
      IgnoreEvents.aborted(name, outcome, attempt)
    }
  }

}