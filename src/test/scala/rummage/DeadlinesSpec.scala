/* DeadlinesSpec.scala
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

import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent._

/**
 * Test suite for the rummage deadline API.
 */
class DeadlinesSpec extends FlatSpec with Matchers {

  import Deadlines._
  import ExecutionContext.Implicits._

  "Deadlines" should "place a hard limit on the amount of time it takes for a future to complete" in {
    complete {Future {waitFor(2 seconds); "hi"} withDeadline 4.seconds} shouldBe "hi"
    a[DeadlineException] should be thrownBy {complete {Future {waitFor(4 seconds); "hi"} withDeadline 2.seconds}}
  }

  def waitFor(duration: FiniteDuration) =
    blocking {Thread.sleep(duration toMillis)}

  def complete[T](future: Future[T]): T =
    Await.result(future, Duration.Inf)

}