/* RetryPolicySpec.scala
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
package atmos

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import rummage.Clock
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

/**
 * Test suite for [[atmos.RetryPolicy]].
 */
class RetryPolicySpec211 extends FlatSpec with Matchers with MockFactory {

  import ExecutionContext.Implicits._
  import dsl._
  import termination._

  "RetryPolicy" should "synchronously retry until complete" in {
    implicit val policy = RetryPolicy(LimitAttempts(2))
    var counter = 0
    retry() {
      counter += 1
      if (counter < 2) throw new TestException
      counter
    } shouldEqual 2
    counter shouldEqual 2
  }

  it should "synchronously retry and swallow silent errors" in {
    val mockMonitor = mock[EventMonitor]
    implicit val policy = RetryPolicy(LimitAttempts(2), monitor = mockMonitor, classifier = {
      case _: TestException => ErrorClassification.SilentlyRecoverable
    })
    val e = new TestException
    (mockMonitor.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean)).expects(None, Failure(e), 1, *, true)
    var counter = 0
    retry() {
      counter += 1
      if (counter < 2) throw e
      counter
    } shouldEqual 2
    counter shouldEqual 2
  }

  it should "synchronously retry until signaled to terminate" in {
    implicit val policy = RetryPolicy(LimitAttempts(2))
    var counter = 0
    a[TestException] should be thrownBy {
      retry("test") {
        counter += 1
        throw new TestException
      }
    }
    counter shouldEqual 2
  }

  it should "synchronously retry until encountering a fatal error" in {
    implicit val policy = RetryPolicy(LimitAttempts(2), classifier = {
      case _: TestException => ErrorClassification.Fatal
    })
    var counter = 0
    a[TestException] should be thrownBy {
      retry(None) {
        counter += 1
        throw new TestException
      }
    }
    counter shouldEqual 1
  }

  it should "synchronously block between retry attempts" in {
    implicit val policy = RetryPolicy(LimitAttempts(2), backoff.ConstantBackoff(1.second))
    val startAt = System.currentTimeMillis
    var counter = 0
    retry(Some("test")) {
      counter += 1
      if (counter < 2) throw new TestException
      counter
    } shouldEqual 2
    counter shouldEqual 2
    (System.currentTimeMillis - startAt).millis should be >= 1.second
  }

  it should "retry when unacceptable results are returned" in {
    implicit val policy = RetryPolicy(LimitAttempts(2), results = ResultClassifier {
      case i: Int if i < 2 => ResultClassification.Unacceptable()
    })
    var counter = 0
    retry(None) {
      counter += 1
      counter
    } shouldEqual 2
    counter shouldEqual 2
  }

  it should "be interrupted by fatal errors by default" in {
    implicit val policy = RetryPolicy(LimitAttempts(2))
    var counter = 0
    an[InterruptedException] should be thrownBy retry(None) {
      counter += 1
      throw new InterruptedException
    }
    counter shouldEqual 1
  }

  it should "asynchronously retry until complete" in {
    implicit val policy = RetryPolicy(LimitAttempts(3))
    @volatile var counter = 0
    val future = retryAsync() {
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
    implicit val policy = RetryPolicy(LimitAttempts(3), monitor = mockMonitor, classifier = {
      case _: TestException => ErrorClassification.SilentlyRecoverable
    })
    val e = new TestException
    (mockMonitor.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean)).expects(None, Failure(e), 1, *, true)
    (mockMonitor.retrying(_: Option[String], _: Try[Any], _: Int, _: FiniteDuration, _: Boolean)).expects(None, Failure(e), 2, *, true)
    @volatile var counter = 0
    val future = retryAsync() {
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
    implicit val policy = RetryPolicy(LimitAttempts(2))
    @volatile var counter = 0
    val future = retryAsync("test") {
      Future {
        counter += 1
        if (counter <= 2) throw new TestException
        counter
      }
    }
    a[TestException] should be thrownBy {Await.result(future, Duration.Inf)}
    counter shouldEqual 2
  }

  it should "asynchronously retry until encountering a fatal error" in {
    implicit val policy = RetryPolicy(LimitAttempts(1), classifier = {
      case _: TestException => ErrorClassification.Fatal
    }) retryFor {2 attempts}
    @volatile var counter = 0
    val future = retryAsync(None) {
      Future {
        counter += 1
        if (counter < 2) throw new TestException
        counter
      }
    }
    a[TestException] should be thrownBy {Await.result(future, Duration.Inf)}
    counter shouldEqual 1
  }

  it should "asynchronously block between retry attempts" in {
    implicit val policy = RetryPolicy(1.minute || 2.attempts, backoff.ConstantBackoff(1.second))
    val startAt = System.currentTimeMillis
    @volatile var counter = 0
    val future = retryAsync(Some("test")) {
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

  it should "predictably terminate in the presence of asynchronous concurrency errors" in {
    implicit val policy = RetryPolicy(LimitAttempts(2), backoff.ConstantBackoff(1.second))
    @volatile var limit = 1
    @volatile var counter = 0
    val mockFuture = new Future[Int] {
      def value = ???

      def isCompleted = ???

      def ready(atMost: Duration)(implicit permit: scala.concurrent.CanAwait) = ???

      def result(atMost: Duration)(implicit permit: scala.concurrent.CanAwait) = ???

      def onComplete[U](f: Try[Int] => U)(implicit executor: ExecutionContext) = {
        counter += 1
        if (counter >= limit)
          throw new TestException
        else
          executor.execute(new Runnable {override def run() = {f(Failure(new TestException))}})
      }
    }
    val future1 = retryAsync() {mockFuture}
    a[TestException] should be thrownBy Await.result(future1, Duration.Inf)
    limit = 2
    counter = 0
    val future2 = retryAsync("test") {mockFuture}
    a[TestException] should be thrownBy Await.result(future2, Duration.Inf)
  }

  it should "predictably terminate in the presence of asynchronous clock errors" in {
    implicit val policy = RetryPolicy(LimitAttempts(3), backoff.ConstantBackoff(1.second))
    @volatile var limit = 1
    @volatile var counter = 0
    implicit val mockClock = new Clock {
      def now: FiniteDuration = Clock.Default.now

      def tick: FiniteDuration = Clock.Default.tick

      def syncWait(timeout: FiniteDuration): FiniteDuration = Clock.Default.syncWait(timeout)

      def asyncWait(timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[FiniteDuration] = {
        counter += 1
        if (counter >= limit)
          throw new TestException
        else
          Future.failed(new TestException)
      }
    }
    val future1 = retryAsync() {Future {throw new RuntimeException}}
    a[TestException] should be thrownBy Await.result(future1, Duration.Inf)
    limit = 2
    counter = 0
    val future2 = retryAsync("test") {Future {throw new RuntimeException}}
    a[TestException] should be thrownBy Await.result(future2, Duration.Inf)
  }

  private class TestException extends RuntimeException

}