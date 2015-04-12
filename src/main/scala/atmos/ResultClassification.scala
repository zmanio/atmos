/* ResultClassification.scala
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
package atmos

/**
 * Base type for result classifications.
 */
sealed trait ResultClassification

/**
 * Definitions of the supported result classifications and the default classification function.
 */
object ResultClassification extends (Any => ResultClassification) {
  
  /** The default status used for unacceptable results. */
  val defaultUnacceptableStatus = ErrorClassification.Recoverable

  /**
   * Returns a classification for the specified result using the default strategy.
   *
   * @param result The result to classify.
   */
  override def apply(result: Any): ResultClassification = Acceptable

  /**
   * The classification of results that will not interrupt the retry operation.
   */
  case object Acceptable extends ResultClassification

  /**
   * The classification of results that will interrupt the retry operation.
   */
  case class Unacceptable(status: ErrorClassification = defaultUnacceptableStatus) extends ResultClassification

}