/* package.scala
 * 
 * Copyright (c) 2013 bizo.com
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
 * The `atmos.retries` package aims to provide a simple, concise mechanism for implementing retry-on-failure behavior.
 * 
 * Retry behavior is controlled by an instance of [[atmos.retries.RetryPolicy]] configured with strategies for various
 * components of the retry process. The strategies that may be configured are:
 * 
 *  - [[atmos.retries.TerminationPolicy]]: Defines when a retry operation should abort and make no further attempts.
 *  - [[atmos.retries.BackoffPolicy]]: Defines how long to wait between successive attempts.
 *  - [[atmos.retries.EventMonitor]]: Defines an interface that is notified of events during a retry operation.
 *  - [[atmos.retries.ErrorClassifier]]: Defines a mechanism to describe how different errors impact a retry operation.
 *  
 * Additionally, [[atmos.retries.RetryDSL]] provides a simple DSL for specifying concise retry policy definitions.
 */
package object retries {

  /** The type of error classifier function. */
  type ErrorClassifier = PartialFunction[Throwable, ErrorClassification]

  /**
   * Common error classifiers.
   */
  object ErrorClassifier {

    /** An error classifier that classifies nothing */
    val empty: ErrorClassifier = PartialFunction.empty
    
    /**
     * Returns the supplied partial function.
     * 
     * @param f The partial function to return.
     */
    def apply(f: PartialFunction[Throwable, ErrorClassification]): ErrorClassifier = f

  }

}