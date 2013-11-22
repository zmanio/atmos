/* RetryPolicySpec.scala
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
 * 
 * Portions of this code are derived from https://github.com/aboisvert/pixii
 * and https://github.com/lpryor/squishy.
 */
package atmos.retries

import scala.concurrent.{ Await, Future, ExecutionContext }
import scala.concurrent.duration._
import org.scalatest._

/**
 * Test suite for [[atmos.retries.RetryPolicy]].
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RetryPolicySpec extends FunSpec with Matchers {

  import ExecutionContext.Implicits._
  import TerminationPolicy._

  describe("RetryPolicy") {

    it("should synchronously retry until complete") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2))
      var counter = 0
      policy.retry() {
        counter += 1
        if (counter < 2) throw new TestException
        counter
      } shouldEqual 2
      counter shouldEqual 2
    }

    it("should synchronously retry until signaled to terminate") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2))
      var counter = 0
      evaluating {
        policy.retry() {
          counter += 1
          if (counter <= 2) throw new TestException
          counter
        }
      } should produce[TestException]
      counter shouldEqual 2
    }

    it("should synchronously retry until encountering a fatal error") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2), classifier = {
        case _: TestException => ErrorClassification.Fatal
      })
      var counter = 0
      evaluating {
        policy.retry() {
          counter += 1
          if (counter < 2) throw new TestException
          counter
        }
      } should produce[TestException]
      counter shouldEqual 1
    }

    it("should synchronously block between retry attempts") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2), BackoffPolicy.Constant(1.second))
      val startAt = System.currentTimeMillis
      var counter = 0
      policy.retry() {
        counter += 1
        if (counter < 2) throw new TestException
        counter
      } shouldEqual 2
      counter shouldEqual 2
      (System.currentTimeMillis - startAt).millis should be >= 1.second
    }

    it("should asynchronously retry until complete") {
      val policy = RetryPolicy(LimitNumberOfAttempts(3))
      @volatile var counter = 0
      val future = policy.retryAsync() {
        if (counter == 0) {
          counter += 1
          throw new TestException
        }
        Future {
          counter += 1
          if (counter < 3) throw new TestException
          counter
        }
      }
      Await.result(future, Duration.Inf) shouldEqual 3
      counter shouldEqual 3
    }

    it("should asynchronously retry until signaled to terminate") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2))
      @volatile var counter = 0
      val future = policy.retryAsync() {
        Future {
          counter += 1
          if (counter <= 2) throw new TestException
          counter
        }
      }
      evaluating { Await.result(future, Duration.Inf) } should produce[TestException]
      counter shouldEqual 2
    }

    it("should asynchronously retry until encountering a fatal error") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2), classifier = {
        case _: TestException => ErrorClassification.Fatal
      })
      @volatile var counter = 0
      val future = policy.retryAsync() {
        Future {
          counter += 1
          if (counter < 2) throw new TestException
          counter
        }
      }
      evaluating { Await.result(future, Duration.Inf) } should produce[TestException]
      counter shouldEqual 1
    }

    it("should asynchronously block between retry attempts") {
      val policy = RetryPolicy(LimitNumberOfAttempts(2), BackoffPolicy.Constant(1.second))
      val startAt = System.currentTimeMillis
      @volatile var counter = 0
      val future = policy.retryAsync() {
        Future {
          counter += 1
          if (counter < 2) throw new TestException
          counter
        }
      }
      Await.result(future, Duration.Inf) shouldEqual 2
      counter shouldEqual 2
      (System.currentTimeMillis - startAt).millis should be >= 1.second
    }

  }

  private class TestException extends RuntimeException

}