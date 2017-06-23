/* FormatEventsSpec.scala
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
package atmos.monitor

import org.scalatest._
import scala.concurrent.duration._
import scala.util.Failure

/**
 * Test suite for [[atmos.monitor.FormatEvents]].
 */
class FormatEventsSpec extends FlatSpec with Matchers {

  val thrownWithMsg = new RuntimeException("ERROR!")
  val thrownWithoutMsg = new RuntimeException("")

  "FormatEvents" should "include relevant information in the formatted messages" in {
    val formatter = new FormatEvents {}
    for {
      name <- Seq(Some("name"), None)
      thrown <- Seq(thrownWithMsg, thrownWithoutMsg)
      attempt <- 1 to 10
    } {
      for (backoff <- 1L to 100L map (100.millis * _))
        checkMessage(name, thrown, attempt, Some(backoff), formatter.formatRetrying(name, Failure(thrown), attempt, backoff))
      checkMessage(name, thrown, attempt, None, formatter.formatInterrupted(name, Failure(thrown), attempt))
      checkMessage(name, thrown, attempt, None, formatter.formatAborted(name, Failure(thrown), attempt))
    }
  }

  def checkMessage(name: Option[String], e: Exception, attempt: Int, backoff: Option[FiniteDuration], msg: String) = {
    msg should include(name getOrElse "operation")
    if (e.getMessage.nonEmpty) msg should include(e.getMessage)
    msg should include(attempt.toString)
    backoff foreach { backoff => msg should include(backoff.toString) }
  }

}