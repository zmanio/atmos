/* EventMonitorSpec.scala
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
package atmos

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.concurrent.duration._
import scala.util.{Failure, Try}

/**
 * Test suite for [[atmos.EventMonitor]].
 */
class EventMonitorSpec extends FlatSpec with Matchers with MockFactory {

  val thrown = new RuntimeException

  "EventMonitor" should "support deprecated APIs" in {
    val fixture = new EventMonitorFixture
    fixture.retrying.expects(None, Failure(thrown), 1, 5 seconds, true)
    fixture.mock.retrying(None, thrown, 1, 5 seconds, true)
    fixture.interrupted.expects(None, Failure(thrown), 2)
    fixture.mock.interrupted(None, thrown, 2)
    fixture.aborted.expects(None, Failure(thrown), 3)
    fixture.mock.aborted(None, thrown, 3)
  }

  class EventMonitorFixture {
    self =>
    val retrying = mockFunction[Option[String], Try[Any], Int, FiniteDuration, Boolean, Unit]
    val interrupted = mockFunction[Option[String], Try[Any], Int, Unit]
    val aborted = mockFunction[Option[String], Try[Any], Int, Unit]
    val mock = new EventMonitor {
      override def retrying(name: Option[String], outcome: Try[Any], attempts: Int, backoff: FiniteDuration, silent: Boolean) =
        self.retrying(name, outcome, attempts, backoff, silent)

      override def interrupted(name: Option[String], outcome: Try[Any], attempts: Int) =
        self.interrupted(name, outcome, attempts)

      override def aborted(name: Option[String], outcome: Try[Any], attempts: Int) =
        self.aborted(name, outcome, attempts)
    }
  }

}