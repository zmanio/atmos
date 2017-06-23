/* ChainedEventsSpec.scala
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
package atmos.monitor

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Test suite for [[atmos.monitor.FormatEvents]].
 */
class ChainedEventsSpec extends FlatSpec with Matchers with MockFactory {

  val result = "result"
  val monitor1 = mock[atmos.EventMonitor]
  val monitor2 = mock[atmos.EventMonitor]
  val chained = ChainedEvents(monitor1, monitor2)
  val thrown = new RuntimeException
  val thrownWithMsg = new IllegalArgumentException("ERROR!")
  val thrownWithoutMsg = new IllegalStateException("")

  "ChainedEvents" should "notify both monitors with no exceptions thrown" in {
    for {
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        (monitor1.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean)).expects(name, outcome, attempt, backoff, silent)
        (monitor2.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean)).expects(name, outcome, attempt, backoff, silent)
        chained.retrying(name, outcome, attempt, backoff, silent)
      }
      (monitor1.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      (monitor2.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      chained.interrupted(name, outcome, attempt)
      (monitor1.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      (monitor2.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      chained.aborted(name, outcome, attempt)
    }
  }

  "ChainedEvents" should "notify both monitors when the first one throws an exception" in {
    for {
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        (monitor1.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent).throws(thrownWithMsg)
        (monitor2.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent)
        an[IllegalArgumentException] should be thrownBy {chained.retrying(name, outcome, attempt, backoff, silent)}
      }
      (monitor1.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithMsg)
      (monitor2.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      an[IllegalArgumentException] should be thrownBy {chained.interrupted(name, outcome, attempt)}
      (monitor1.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithMsg)
      (monitor2.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      an[IllegalArgumentException] should be thrownBy {chained.aborted(name, outcome, attempt)}
    }
  }

  "ChainedEvents" should "notify both monitors when the second one throws an exception" in {
    for {
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        (monitor1.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent)
        (monitor2.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent).throws(thrownWithoutMsg)
        an[IllegalStateException] should be thrownBy {chained.retrying(name, outcome, attempt, backoff, silent)}
      }
      (monitor1.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      (monitor2.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithoutMsg)
      an[IllegalStateException] should be thrownBy {chained.interrupted(name, outcome, attempt)}
      (monitor1.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
      (monitor2.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithoutMsg)
      an[IllegalStateException] should be thrownBy {chained.aborted(name, outcome, attempt)}
    }
  }

  "ChainedEvents" should "notify both monitors when the both throw an exception" in {
    for {
      name <- Seq(Some("name"), None)
      attempt <- 1 to 10
      outcome <- Seq(Success(result), Failure(thrown))
    } {
      for {
        backoff <- 1L to 100L map (100.millis * _)
        silent <- Seq(true, false)
      } {
        (monitor1.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent).throws(thrownWithMsg)
        (monitor2.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean))
          .expects(name, outcome, attempt, backoff, silent).throws(thrownWithoutMsg)
        an[IllegalArgumentException] should be thrownBy {chained.retrying(name, outcome, attempt, backoff, silent)}
      }
      (monitor1.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithMsg)
      (monitor2.interrupted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithoutMsg)
      an[IllegalArgumentException] should be thrownBy {chained.interrupted(name, outcome, attempt)}
      (monitor1.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithMsg)
      (monitor2.aborted(_: Option[String], _: Try[Any], _: Int)).expects(name, outcome, attempt)
        .throws(thrownWithoutMsg)
      an[IllegalArgumentException] should be thrownBy {chained.aborted(name, outcome, attempt)}
    }
  }

}