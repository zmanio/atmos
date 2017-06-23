/* AkkaSupportSpec.scala
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

import akka.event.{Logging, LoggingAdapter}
import atmos.dsl.AkkaSupport._
import atmos.monitor._
import org.scalatest._

/**
 * Test suite for [[atmos.dsl.AkkaSupport]].
 */
class AkkaSupportSpec extends FlatSpec with Matchers {

  "AkkaSupport" should "support viewing Akka-logging-compatible objects as event monitors" in {
    val monitor = MockAdapter: LogEventsWithAkka
    monitor shouldBe LogEventsWithAkka(MockAdapter)
    (monitor: LogEventsWithAkkaExtensions) shouldBe LogEventsWithAkkaExtensions(monitor)
    (MockAdapter: LogEventsWithAkkaExtensions) shouldBe LogEventsWithAkkaExtensions(LogEventsWithAkka(MockAdapter))
  }

  it should "return Akka log levels in response to generic level queries" in {
    import AkkaSupport.AkkaEventLogLevels
    AkkaEventLogLevels.errorLevel shouldBe Logging.ErrorLevel
    AkkaEventLogLevels.warningLevel shouldBe Logging.WarningLevel
    AkkaEventLogLevels.infoLevel shouldBe Logging.InfoLevel
    AkkaEventLogLevels.debugLevel shouldBe Logging.DebugLevel
  }

  object MockAdapter extends LoggingAdapter {
    def isDebugEnabled = ???

    def isErrorEnabled = ???

    def isInfoEnabled = ???

    def isWarningEnabled = ???

    def notifyDebug(message: String) = ???

    def notifyError(cause: Throwable, message: String) = ???

    def notifyError(message: String) = ???

    def notifyInfo(message: String) = ???

    def notifyWarning(message: String) = ???
  }

}