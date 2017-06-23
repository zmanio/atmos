/* package.scala
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

/**
 * The `atmos` package aims to provide a concise mechanism for implementing retry-on-failure behavior.
 *
 * Retry behavior is controlled by an instance of [[atmos.RetryPolicy]] configured with strategies for various
 * components of the retry operation. The elements that define a retry policy are:
 *
 *  - [[atmos.ResultClassifier]]: Defines what results are acceptable for a retry operation to return.
 *
 *  - [[atmos.ErrorClassifier]]: Defines when a retry operation should be interrupted by a fatal error.
 *
 *  - [[atmos.BackoffPolicy]]: Defines how long to wait between successive retry attempts. The [[atmos.backoff]]
 * package provides a number of common backoff policy implementations.
 *
 *  - [[atmos.EventMonitor]]: An interface that is notified of events during a retry operation. The [[atmos.monitor]]
 * package provides a number of common event monitor implementations.
 *
 *  - [[atmos.TerminationPolicy]]: Defines when a retry operation should abort and make no further attempts. The
 * [[atmos.termination]] package provides a number of common termination policy implementations.
 *
 * Additionally, the [[atmos.dsl]] package provides a concise DSL for describing retry policies.
 *
 * For more information about using the `atmos` library, see [[http://zman.io/atmos]]
 */
package object atmos {

  /** The type of result classifier functions. */
  type ResultClassifier = PartialFunction[Any, ResultClassification]

  /**
   * Common result classifiers.
   */
  object ResultClassifier {

    /** A result classifier that classifies nothing. */
    val empty: ResultClassifier = PartialFunction.empty

    /**
     * Returns the supplied partial function.
     *
     * @param f The partial function to return.
     */
    def apply(f: PartialFunction[Any, ResultClassification]): ResultClassifier = f

  }

  /** The type of error classifier functions. */
  type ErrorClassifier = PartialFunction[Throwable, ErrorClassification]

  /**
   * Common error classifiers.
   */
  object ErrorClassifier {

    /** An error classifier that classifies nothing. */
    val empty: ErrorClassifier = PartialFunction.empty

    /**
     * Returns the supplied partial function.
     *
     * @param f The partial function to return.
     */
    def apply(f: PartialFunction[Throwable, ErrorClassification]): ErrorClassifier = f

  }

}