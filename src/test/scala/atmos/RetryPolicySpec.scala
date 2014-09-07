/* RetryPolicySpec.scala
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
package atmos

import scala.concurrent.{ ExecutionContext, Future, Await }
import scala.concurrent.duration._
import org.scalatest._
import org.scalamock.scalatest.MockFactory

/**
 * Test suite for [[atmos.RetryPolicy]].
 */
class RetryPolicySpec extends FlatSpec with Matchers with MockFactory {

  import ExecutionContext.Implicits._
  import termination._

  "RetryPolicy" should "synchronously retry until complete" in {
    val policy = RetryPolicy(LimitAttempts(2))
    var counter = 0
    policy.retry() {
      counter += 1
      if (counter < 2) throw new TestException
      counter
    } shouldEqual 2
    counter shouldEqual 2
  }

  it should "synchronously retry and swallow silent errors" in {
    val mockMonitor = mock[EventMonitor]
    val policy = RetryPolicy(LimitAttempts(2), monitor = mockMonitor, classifier = {
      case _: TestException => ErrorClassification.SilentlyRecoverable
    })
    val e = new TestException
    (mockMonitor.retrying _).expects(None, e, 1, *, true)
    var counter = 0
    policy.retry() {
      counter += 1
      if (counter < 2) throw e
      counter
    } shouldEqual 2
    counter shouldEqual 2
  }

  it should "synchronously retry until signaled to terminate" in {
    val policy = RetryPolicy(LimitAttempts(2))
    var counter = 0
    evaluating {
      policy.retry("test") {
        counter += 1
        throw new TestException
      }
    } should produce[TestException]
    counter shouldEqual 2
  }

  it should "synchronously retry until encountering a fatal error" in {
    val policy = RetryPolicy(LimitAttempts(2), classifier = {
      case _: TestException => ErrorClassification.Fatal
    })
    var counter = 0
    evaluating {
      policy.retry(None) {
        counter += 1
        throw new TestException
      }
    } should produce[TestException]
    counter shouldEqual 1
  }

  it should "synchronously block between retry attempts" in {
    val policy = RetryPolicy(LimitAttempts(2), backoff.ConstantBackoff(1.second))
    val startAt = System.currentTimeMillis
    var counter = 0
    policy.retry(Some("test")) {
      counter += 1
      if (counter < 2) throw new TestException
      counter
    } shouldEqual 2
    counter shouldEqual 2
    (System.currentTimeMillis - startAt).millis should be >= 1.second
  }

  it should "asynchronously retry until complete" in {
    val policy = RetryPolicy(LimitAttempts(3))
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

  it should "asynchronously retry and swallow silent errors" in {
    val mockMonitor = mock[EventMonitor]
    val policy = RetryPolicy(LimitAttempts(3), monitor = mockMonitor, classifier = {
      case _: TestException => ErrorClassification.SilentlyRecoverable
    })
    val e = new TestException
    (mockMonitor.retrying _).expects(None, e, 1, *, true)
    (mockMonitor.retrying _).expects(None, e, 2, *, true)
    @volatile var counter = 0
    val future = policy.retryAsync() {
      if (counter == 0) {
        counter += 1
        throw e
      }
      Future {
        counter += 1
        if (counter < 3) throw e
        counter
      }
    }
    Await.result(future, Duration.Inf) shouldEqual 3
    counter shouldEqual 3
  }

  it should "asynchronously retry until signaled to terminate" in {
    val policy = RetryPolicy(LimitAttempts(2))
    @volatile var counter = 0
    val future = policy.retryAsync("test") {
      Future {
        counter += 1
        if (counter <= 2) throw new TestException
        counter
      }
    }
    evaluating { Await.result(future, Duration.Inf) } should produce[TestException]
    counter shouldEqual 2
  }

  it should "asynchronously retry until encountering a fatal error" in {
    val policy = RetryPolicy(LimitAttempts(2), classifier = {
      case _: TestException => ErrorClassification.Fatal
    })
    @volatile var counter = 0
    val future = policy.retryAsync(None) {
      Future {
        counter += 1
        if (counter < 2) throw new TestException
        counter
      }
    }
    evaluating { Await.result(future, Duration.Inf) } should produce[TestException]
    counter shouldEqual 1
  }

  it should "asynchronously block between retry attempts" in {
    val policy = RetryPolicy(LimitAttempts(2), backoff.ConstantBackoff(1.second))
    val startAt = System.currentTimeMillis
    @volatile var counter = 0
    val future = policy.retryAsync(Some("test")) {
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

  private class TestException extends RuntimeException

}