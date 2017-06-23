/* TrySpec.scala
 * 
 * Copyright (c) 2014-2015 linkedin.com
 * Copyright (c) 2014-2015 zman.io
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
import scala.language.postfixOps
import scala.util.{Success, Try}

/**
 * Test suite for the rummage try extensions.
 */
class TrySpec extends FlatSpec with Matchers {

  import TryAll._

  "The try extensions" should "support composing operations" in {
    val chars: Try[List[Char]] = Success("hi" toList)
    chars.onEach map upperChar shouldBe Success("HI" toList)
    chars.onEach flatMap upperCharSuccess shouldBe Success("HI" toList)
    chars.onEach flatMap upperCharProblem match {case Problem("h") =>}
    chars.onEvery map upperChar shouldBe Success("HI" toList)
    chars.onEvery flatMap upperCharSuccess shouldBe Success("HI" toList)
    chars.onEvery flatMap upperCharProblem match {case Failures(Vector(Problem("h"), Problem("i"))) =>}
  }

  /** Convert a character to upper-case. */
  def upperChar(c: Char): Char = Character toUpperCase c

  /** Successfully convert a character to upper-case. */
  def upperCharSuccess(c: Char): Try[Char] = Success(upperChar(c))

  /** Fail to convert a character to upper-case. */
  def upperCharProblem(c: Char): Try[Char] = Problem(c toString)

}