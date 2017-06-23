/* PrintEventsWithStreamSpec.scala
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

import java.io.{ByteArrayOutputStream, PrintStream}
import org.scalatest._

/**
 * Test suite for [[atmos.monitor.PrintEventsWithStream]].
 */
class PrintEventsWithStreamSpec extends FlatSpec with Matchers {

  val encoding = "UTF-8"
  val message = "MSG"
  val thrown = new RuntimeException

  "PrintEventsWithStream" should "forward messages and stack traces to a print stream" in {
    val fixture = new StreamFixture
    val monitor = PrintEventsWithStream(fixture.stream)
    monitor.printMessage(message)
    fixture.recover() shouldBe new StreamFixture().message(message)
    monitor.printMessageAndStackTrace(message, thrown)
    fixture.recover() shouldBe new StreamFixture().messageAndStackTrace(message, thrown)
  }

  class StreamFixture {
    val baos = new ByteArrayOutputStream
    val stream = new PrintStream(baos, false, encoding)

    def message(msg: String) = {
      stream.println(msg)
      recover()
    }

    def messageAndStackTrace(msg: String, thrown: Throwable) = {
      stream.println(msg)
      thrown.printStackTrace(stream)
      recover()
    }

    def recover() = {
      stream.flush()
      val result = baos.toString(encoding)
      baos.reset()
      result
    }
  }

}