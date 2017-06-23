/* ErrorClassification.scala
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
package atmos

import scala.util.control.NonFatal

/**
 * Base type for error classifications.
 */
sealed trait ErrorClassification {

  /** True if this classification will interrupt the retry operation. */
  def isFatal: Boolean = false

  /** True if this classification will not record information about a failure. */
  def isSilent: Boolean = false

}

/**
 * Definitions of the supported error classifications and the default classification function.
 */
object ErrorClassification extends (Throwable => ErrorClassification) {

  /**
   * Returns a classification for the specified error using the default strategy.
   *
   * @param thrown The error to classify.
   */
  override def apply(thrown: Throwable): ErrorClassification = if (NonFatal(thrown)) Recoverable else Fatal

  /**
   * The classification of errors that will interrupt the retry operation.
   */
  case object Fatal extends ErrorClassification {override def isFatal = true}

  /**
   * The classification of errors that will not interrupt the retry operation.
   */
  case object Recoverable extends ErrorClassification

  /**
   * The classification of errors that will not interrupt the retry operation or record information about the failure.
   */
  case object SilentlyRecoverable extends ErrorClassification {override def isSilent = true}

}