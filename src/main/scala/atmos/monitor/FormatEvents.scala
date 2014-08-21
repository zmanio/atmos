/* FormatEvents.scala
 * 
 * Copyright (c) 2013-2014 bizo.com
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

import scala.concurrent.duration.FiniteDuration

/**
 * A mix-in that formats messages for retry events.
 */
trait FormatEvents {

  /**
   * Formats a message for a retrying event.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that have been made so far.
   * @param backoff The amount of time that will pass before another attempt is made.
   */
  def formatRetrying(name: Option[String], thrown: Throwable, attempts: Int, backoff: FiniteDuration): String = {
    val op = name getOrElse "operation"
    val tpe = thrown.getClass.getName
    val suffix = Option(thrown.getMessage) filter (_.nonEmpty) map (msg => s": $msg") getOrElse ""
    s"""|Attempt $attempts of $op failed: $tpe$suffix
        |Retrying after $backoff ...""".stripMargin
  }

  /**
   * Formats a message for an interrupted event.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that were made.
   */
  def formatInterrupted(name: Option[String], thrown: Throwable, attempts: Int): String = {
    val op = name getOrElse "operation"
    val tpe = thrown.getClass.getName
    val suffix = Option(thrown.getMessage) filter (_.nonEmpty) map (msg => s": $msg") getOrElse ""
    s"Attempt $attempts of $op interrupted: $tpe$suffix"
  }

  /**
   * Formats a message for an aborted event.
   *
   * @param name The name of the operation that failed if one was provided.
   * @param thrown The exception that was thrown.
   * @param attempts The number of attempts that were made.
   */
  def formatAborted(name: Option[String], thrown: Throwable, attempts: Int): String = {
    val op = name getOrElse "operation"
    val tpe = thrown.getClass.getName
    val suffix = Option(thrown.getMessage) filter (_.nonEmpty) map (msg => s": $msg") getOrElse ""
    s"Too many exceptions after attempt $attempts of $op... aborting: $tpe$suffix"
  }

}