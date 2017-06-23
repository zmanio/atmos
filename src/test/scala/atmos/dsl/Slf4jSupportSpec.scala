/* Slf4jSupportSpec.scala
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
package atmos.dsl

import atmos.dsl.Slf4jSupport._
import atmos.monitor._
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.slf4j.Logger

/**
 * Test suite for [[atmos.dsl.Slf4jSupport]].
 */
class Slf4jSupportSpec extends FlatSpec with Matchers with MockFactory {

  "Slf4jSupport" should "support viewing Slf4j-logging-compatible objects as event monitors" in {
    val logger = mock[Logger]
    val monitor = logger: LogEventsWithSlf4j
    monitor shouldBe LogEventsWithSlf4j(logger)
    (monitor: LogEventsWithSlf4jExtensions) shouldBe LogEventsWithSlf4jExtensions(monitor)
    (logger: LogEventsWithSlf4jExtensions) shouldBe LogEventsWithSlf4jExtensions(LogEventsWithSlf4j(logger))
  }

  it should "return Slf4j log levels in response to generic level queries" in {
    Slf4jEventLogLevels.errorLevel shouldBe LogEventsWithSlf4j.Slf4jLevel.Error
    Slf4jEventLogLevels.warningLevel shouldBe LogEventsWithSlf4j.Slf4jLevel.Warn
    Slf4jEventLogLevels.infoLevel shouldBe LogEventsWithSlf4j.Slf4jLevel.Info
    Slf4jEventLogLevels.debugLevel shouldBe LogEventsWithSlf4j.Slf4jLevel.Debug
  }

}