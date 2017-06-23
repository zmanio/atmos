/* FormatEvents.scala
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

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
 * A mix-in that formats messages for retry events.
 */
trait FormatEvents {

  /**
   * Formats a message for a retrying event.
   *
   * @param name     The name of the operation that failed if one was provided.
   * @param outcome  The outcome of the most recent retry attempt.
   * @param attempts The number of attempts that have been made so far.
   * @param backoff  The amount of time that will pass before another attempt is made.
   */
  def formatRetrying(name: Option[String], outcome: Try[Any], attempts: Int, backoff: FiniteDuration): String = {
    val op = name getOrElse "operation"
    val message = toMessage(outcome)
    s"""|Attempt $attempts of $op failed: $message
        |Retrying after $backoff ...""" stripMargin
  }

  /**
   * Formats a message for an interrupted event.
   *
   * @param name     The name of the operation that failed if one was provided.
   * @param outcome  The outcome of the most recent retry attempt.
   * @param attempts The number of attempts that were made.
   */
  def formatInterrupted(name: Option[String], outcome: Try[Any], attempts: Int): String = {
    val op = name getOrElse "operation"
    val message = toMessage(outcome)
    s"Attempt $attempts of $op interrupted: $message"
  }

  /**
   * Formats a message for an aborted event.
   *
   * @param name     The name of the operation that failed if one was provided.
   * @param outcome  The outcome of the most recent retry attempt.
   * @param attempts The number of attempts that were made.
   */
  def formatAborted(name: Option[String], outcome: Try[Any], attempts: Int): String = {
    val op = name getOrElse "operation"
    val message = toMessage(outcome)
    s"Too many exceptions after attempt $attempts of $op... aborting: $message"
  }

  /**
   * Converts the provided outcome to a message describing the outcome.
   *
   * @param outcome The outcome of the most recent retry attempt.
   */
  private def toMessage(outcome: Try[Any]): String = outcome match {
    case Success(result) =>
      String valueOf result
    case Failure(thrown) =>
      thrown.getClass.getName + {Option(thrown getMessage) filter {_ nonEmpty} map { msg => ": " + msg } getOrElse ""}
  }

}