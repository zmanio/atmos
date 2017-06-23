/* PrintEventsWithWriterSpec.scala
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

import java.io.{ByteArrayOutputStream, OutputStreamWriter, PrintWriter}
import org.scalatest._

/**
 * Test suite for [[atmos.monitor.PrintEventsWithWriter]].
 */
class PrintEventsWithWriterSpec extends FlatSpec with Matchers {

  val encoding = "UTF-8"
  val message = "MSG"
  val thrown = new RuntimeException

  "PrintEventsWithWriter" should "forward messages and stack traces to a print writer" in {
    val fixture = new WriterFixture
    val monitor = PrintEventsWithWriter(fixture.writer)
    monitor.printMessage(message)
    fixture.recover() shouldBe new WriterFixture().message(message)
    monitor.printMessageAndStackTrace(message, thrown)
    fixture.recover() shouldBe new WriterFixture().messageAndStackTrace(message, thrown)
  }

  class WriterFixture {
    val baos = new ByteArrayOutputStream
    val writer = new PrintWriter(new OutputStreamWriter(baos, encoding))

    def message(msg: String) = {
      writer.println(msg)
      recover()
    }

    def messageAndStackTrace(msg: String, thrown: Throwable) = {
      writer.println(msg)
      thrown.printStackTrace(writer)
      recover()
    }

    def recover() = {
      writer.flush()
      val result = baos.toString(encoding)
      baos.reset()
      result
    }
  }

}