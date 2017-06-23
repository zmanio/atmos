# Forked version of Atmos

![](https://travis-ci.org/paradoxical-io/atmos.svg?branch=master)

Original repo located at: https://github.com/zmanio/atmos

---

A [Scala](http://www.scala-lang.org/) library for describing retry-on-failure behavior using a concise, literate [embedded DSL](http://c2.com/cgi/wiki?EmbeddedDomainSpecificLanguage).

There are places in most modern software where small, intermittent errors can occur and disrupt the normal flow of execution. Traditionally, a *retry loop* is used in these situations, with constraints on the number of attempts that can be made and how certain results and errors are handled. A naive retry loop that would make up to three attempts, waiting 100 milliseconds between each attempt and accepting only non-empty results could look like this:

```scala
import scala.concurrent.duration._

def doSomethingUntilItWorks(): String = {
  val maxAttempts = 3
  val backoff = 100 millis
  var attempts = 0
  while (true) {
    attempts += 1
    try {
      val result = doSomethingThatMightFail()
      if (result nonEmpty) {
        return result
      } else {
        println("Retrying after empty result")
        Thread.sleep(backoff toMillis)
      }
      return 
    } catch {
      case e: SomeImportantException =>
        println("Interrupted by important exception: " + e.getMessage)
        throw e
      case NonFatal(e) =>
        if (attempts >= maxAttempts) {
          println("Aborting after too many attempts: " + e.getMessage)
          throw e
        }
        println("Retrying after error: " + e.getMessage)
        Thread.sleep(backoff toMillis)
      case e =>
        println("Interrupted by fatal exception: " + e.getMessage)
        throw e
    }
  }
  sys.error("unreachable")
}

def doSomethingThatMightFail(): String
```

Retry loops like the one above have a number of problems:

 - They obscure the actual work that the program is trying to do (the lone call to `doSomethingThatMightFail()` above).

 - They are convoluted and tend to contain mutable state, making them hard to reason about and resistant to change.

 - They are difficult and tedious to test, possibly leading to undiscovered bugs in the code base.

With the atmos library, a *retry policy* can be described using a minimalistic DSL, replacing unreadable retry loops with concise, descriptive text. The above example can be described using atmos like so:

```scala
import atmos.dsl._
import scala.concurrent.duration._

implicit val retryPolicy = retryFor { 3 attempts } using constantBackoff { 100 millis } monitorWith System.out onResult {
  case str: String if str isEmpty => rejectResult
} onError {
  case _: SomeImportantException => stopRetrying
}

def doSomethingThatMightFail(): String

val result = retry() { doSomethingThatMightFail() }
```

In addition to making retry behavior easy to understand, atmos provides the ability to customize the strategies that control [loop termination](#termination-policies), [backoff calculation](#backoff-policies), [event monitoring](#event-monitors), [result acceptance](#result-classifiers), [error handling](#error-classifiers), as well as supporting both [synchronous](#retrying-synchronously) and [asynchronous](#retrying-asynchronously) styles of programming. See the [user guide](#using-the-library) below for information about the wide array of customization options this library supports.

[code](https://github.com/zmanio/atmos) - [licence](https://github.com/zmanio/atmos/blob/master/LICENSE) - [api](http://zman.io/atmos/api/#atmos.package) - [history](changelog/)

[![Build Status](https://travis-ci.org/zmanio/atmos.png?branch=master)](https://travis-ci.org/zmanio/atmos) [![Coverage Status](https://coveralls.io/repos/zmanio/atmos/badge.png)](https://coveralls.io/r/zmanio/atmos)

## Table of Contents

 - [Getting Started](#getting-started)
 - [Using the Library](#using-the-library)
   - [Termination Policies](#termination-policies)
   - [Backoff Policies](#backoff-policies)
   - [Event Monitors](#event-monitors)
   - [Result Classifiers](#result-classifiers)
   - [Error Classifiers](#error-classifiers)
   - [Retrying Synchronously](#retrying-synchronously)
   - [Retrying Asynchronously](#retrying-asynchronously)
   - [Retrying with Actors](#retrying-with-actors)
 - [Building and Testing](#building-and-testing)

<a name="getting-started"></a>

## Getting Started

Prerequisites:

 - [Java](http://www.oracle.com/technetwork/java/index.html) 1.6+

 - [Scala](http://scala-lang.org/) 2.10.4+ or Scala 2.11.6+

To use from SBT, add the following to your build.sbt file:

```scala
libraryDependencies += "io.zman" %% "atmos" % "2.1"
```

For other build systems or to download the jar see [atmos in the central repository](http://mvnrepository.com/artifact/io.zman/atmos_2.11/2.1).

<a name="using-the-library"></a>

## Using the Library

The atmos library divides the definition of a retry policy into five parts:

 - [Termination policies](#termination-policies) enforce an upper bound on the number of retry attempts.
 - [Backoff policies](#backoff-policies) calculate the delay that is inserted between retry attempts.
 - [Event monitors](#event-monitors) are notified of events that occur during a retry operation.
 - [Result classifiers](#result-classifiers) determine if a result warrants further attempts.
 - [Error classifiers](#error-classifiers) determine if an error prevents further attempts.

Using the naive retry loop from above, we can classify its behavior according to the five elements of a retry policy:


```scala
while (true) {
  attempts += 1                                // Termination policy
  try {
      val result = doSomethingThatMightFail()
      if (result.length > 0) {                 // Result classifier
        return result
      } else {
        println("Retrying after empty result") // Event monitor
        Thread.sleep(backoff toMillis)         // Backoff policy
      }
  } catch {
    case e: SomeImportantException =>          // Error classifier
      println("interrupted")                   // Event monitor
      throw e
    case NonFatal(e) =>                        // Error classifier
      if (attempts >= maxAttempts) {           // Termination policy
        println("aborting")                    // Event monitor
        throw e
      }
      println("retrying")                      // Event monitor
      Thread.sleep(backoff toMillis)           // Backoff policy
    case e =>
      println("interrupted")                   // Event monitor
      throw e
  }
}
```

Atmos decomposes the traditional retry loop into these five, independent strategies and allows you to easily recombine them in whatever fashion you see fit. A reconstructed retry policy is encapsulated in the [`atmos.RetryPolicy`](http://zman.io/atmos/api/#atmos.RetryPolicy) class.

<a name="termination-policies"></a>

### Termination Policies

Termination policies determine when a retry operation will make no further attempts. Any type that implements the [`atmos.TerminationPolicy`](http://zman.io/atmos/api/#atmos.TerminationPolicy) trait can be used in a retry policy, but the DSL exposes factory methods for creating the most common implementations. DSL methods that define termination policies return a [`RetryPolicy`](http://zman.io/atmos/api/#atmos.RetryPolicy) configured with that termination policy and with default values for its other properties.

A default retry policy that limits an operation to 3 attempts can be created with `retrying`:

```scala
import atmos.dsl._

// Default policy terminates after 3 attempts.
implicit val retryPolicy = retrying
```

Custom termination policies can be specified using `retryFor`:

```scala
import scala.concurrent.duration._
import atmos.dsl._

// Terminate after 5 failed attempts.
implicit val retryPolicy = retryFor { 5 attempts }

// Terminate after retrying for at least 5 minutes.
val otherRetryPolicy = retryFor { 5 minutes }
```

Note that the `5 minutes` parameter is an instance of `scala.concurrent.duration.FiniteDuration` and that any instance of that class may be used as a policy in `retryFor`.

When creating a retry policy using `retryFor`, parameters can be combined using the `&&` and `||` operators to describe complex termination conditions:

```scala
import scala.concurrent.duration._
import atmos.dsl._

// Terminate after 5 failed attempts or retrying for at least 5 minutes, whichever comes first.
implicit val retryPolicy = retryFor { 5.attempts || 5.minutes }

// Terminate after at least 5 failed attempts but not before retrying for at least 5 minutes.
val otherRetryPolicy = retryFor { 5.attempts && 5.minutes }
```

Finally, a retry policy that immediately terminates can be created with `neverRetry` and a retry policy that never terminates (unless directed to by an error classifier) can be created with `retryForever`:

```scala
import atmos.dsl._

// Immediately terminates.
implicit val retryPolicy = neverRetry

// Only terminates if a fatal error is encountered.
val otherRetryPolicy = retryForever
```

<a name="backoff-policies"></a>

### Backoff Policies

Backoff policies specify the delay before subsequent retry attempts and are configured by calling `using` on an existing retry policy. Any type that implements the [`atmos.BackoffPolicy`](http://zman.io/atmos/api/#atmos.BackoffPolicy) trait can be used in a retry policy, but the DSL exposes factory methods for creating the most common implementations.

There are four basic backoff policies provided by this library:

```scala
import scala.concurrent.duration._
import atmos.dsl._

// Wait 5 milliseconds between each attempt.
implicit val retryPolicy = retryForever using constantBackoff { 5 millis }

// Wait 5 seconds after the first attempt, then 10 seconds, then 15 seconds and so on.
val otherRetryPolicy = retryForever using linearBackoff { 5 seconds }

// Wait 5 minites after the first attempt, then 10 seconds, then 20 seconds and so on.
val anotherRetryPolicy = retryForever using exponentialBackoff { 5 minutes }

// Wait 5 hours after the first attempt, then repeatedly multiply by the golden ratio after subsequent attempts.
val yetAnotherRetryPolicy = retryForever using fibonacciBackoff { 5 hours }
```

For each of the above policy declarations, the parameter list may be omitted and the default backoff duration of 100 milliseconds will be used:

```scala
import atmos.dsl._

// Wait the default 100 milliseconds between each attempt.
implicit val retryPolicy = retryForever using constantBackoff

```

Additionally, you can select the type of backoff to use based on the exception that caused the most recent attempt to fail:

```scala
import scala.concurrent.duration._
import atmos.dsl._

// Waits longer after attempts that result in a hypothetical rate throttling exception.
implicit val retryPolicy = retryForever using selectedBackoff {
  case e: ThrottledException => constantBackoff { 5 seconds }
  case _ => constantBackoff
}
```

Finally, any backoff policy can be configured so that each successive backoff duration is perturbed by a random value:

```scala
import scala.concurrent.duration._
import atmos.dsl._

// Randomizes each backoff duration by adding a random duration between 0 milliseconds and 100 milliseconds.
implicit val retryPolicy = retryForever using { constantBackoff { 1 second } randomized 100.millis }

// Randomizes each backoff duration by adding a random duration between -30 seconds and 30 seconds.
val otherRetryPolicy = retryForever using { linearBackoff { 5 minutes } randomized -30.seconds -> 30.seconds }
```

<a name="event-monitors"></a>

### Event Monitors

Event monitors are notified when retry attempts fail and are configured on a retry policy using `monitorWith` and `alsoMonitorWith`.  Any type that implements the [`atmos.EventMonitor`](http://zman.io/atmos/api/#atmos.EventMonitor) trait can be used in a retry policy, but the DSL exposes factory methods for creating the most common implementations.

Event monitors handle three distinct types of events:
 - Retrying events occur when an attempt has failed but another attempt is going to be made.
 - Interrupted events occur when an attempt has failed with a fatal error.
 - Aborted events occur when too many attempts have been made and failed.

This library supports using instances of Java's `PrintStream` and `PrintWriter` as targets for logging retry events. The specifics of what is printed can be customized for each type of event:

```scala
import atmos.dsl._

// Print information about failed attempts to stderr using the default printing strategies.
implicit val retryPolicy = retryForever monitorWith System.err

// Print information about failed attempts to a file, customizing what events get printed and how.
val otherRetryPolicy = retryForever monitorWith {
  new PrintWriter("myFile.log") onRetrying printNothing onInterrupted printMessage onAborted printMessageAndStackTrace
}
```

Additionally, common logging frameworks can be used as event monitors and can be customized for each type of event:

```scala
import java.util.logging.Logger
import atmos.dsl._

// Submit information about failed attempts to the specified instance of `java.util.logging.Logger`.
implicit val retryPolicy = retryForever monitorWith Logger.getLogger("MyLoggerName")

// Submit information about failed attempts to the specified instance of `java.util.logging.Logger`, customizing
// what events get logged and and at what level.
val otherRetryPolicy = retryForever monitorWith {
  Logger.getLogger("MyLoggerName") onRetrying logNothing onInterrupted logWarning onAborted logError
}

// Submit information about failed attempts to the specified instance of `org.slf4j.Logger`, customizing what
// events get logged and and at what level.
import org.slf4j.LoggerFactory
import Slf4jSupport._
val slf4jRetryPolicy = retryForever monitorWith {
  LoggerFactory.getLogger("MyLoggerName") onRetrying logNothing onInterrupted logWarning onAborted logError
}

// Submit information about failed attempts to the specified instance of `akka.event.LoggingAdapter`, customizing
// what events get logged and and at what level.
import akka.event.Logging
import AkkaSupport._
val akkaRetryPolicy = retryForever monitorWith {
  Logging(context.system, this) onRetrying logNothing onInterrupted logWarning onAborted logError
}
```

The behavior of event monitors can be customized based on the outcome of the most recent attempt:

```scala
import atmos.dsl._
import scala.util.Failure
import java.util.logging.Logger

// When interrupted, print the error messsage unless the operation failed with a RuntimeException, in which case both
// the message and stack trace are printed.
val printingRetryPolicy = retryForever monitorWith {
  System.err onInterrupted printMessage onInterruptedWhere {
    case Failure(e: RuntimeException) => printMessageAndStackTrace
  }
}

// Do the same thing as above but for aborted events and with simpler syntax.
val similarPrintingRetryPolicy = retryForever monitorWith {
  System.err onAborted printMessage onAbortedWith [RuntimeException] printMessageAndStackTrace
}

// It is possible to compose multiple monitoring customizations by chaining outcome analyzers using the `or` version
//  of the DSL methods. Monitor customizations are also supported on Slf4j and Akka loggers.
val loggingRetryPolicy = retryForever monitorWith {
  Logger.getLogger("log") onRetrying logDebug onRetryingWith [RuntimeException] logInfo orOnRetryingWhere {
    case Failure(e: Error) => logWarning
  }
}

// NOTE: In Scala 2.10 the `onXxxWith[Throwable]` methods require chaining method invocations with `.`, `(` and `)`.
val sameLoggingRetryPolicy = retryForever monitorWith {
  Logger.getLogger("log").onRetrying(logDebug).onRetryingWith[RuntimeException](logInfo).orOnRetryingWhere {
    case Failure(e: Error) => logWarning
  }
}
```

Finally, multiple event monitors can be chained together and each monitor will be notified of every event:

```scala

import java.util.logging.Logger
import atmos.dsl._

// Submit information about failed attempts to stderr as well as an instance of `java.util.logging.Logger`.
implicit val retryPolicy = retryForever monitorWith System.err alsoMonitorWith Logger.getLogger("MyLoggerName")
```

<a name="result-classifiers"></a>

### Result Classifiers

Results that occur during a retry attempt can be classified as `Acceptable` or `Unaccptable`. `Acceptable` results will be immediately returned by the retry operation. `Unaccptable` results will be logged or suppressed so that the retry operation can continue if the status associated with the result indicates so. Result classifications are defined in [`atmos.ResultClassification`](http://zman.io/atmos/api/#atmos.ResultClassification).

Result classifiers are simply implementations of `PartialFunction` that map instances of `Any` to the desired result classification. In situations where a classifier is not defined for a particular result, any result is considered `Acceptable`. The appropriate partial function type is defined as [`atmos.ResultClassifier`](http://zman.io/atmos/api/#atmos.ResultClassifier) and includes a factory in the companion object.

Result classifiers are configured by calling `onResult` to replace the classifier on an existing retry policy, or by using `orOnResult` to chain a result classifier to the one that a retry policy already contains:

```scala
import atmos.dsl._

// Do not accept any empty results.
implicit val retryPolicy = retryForever onResult { case str: String if str isEmpty => rejectResult }

// Extend the above policy by silently rejecting whitespace only results but rejecting and loggging all results that are too long.
val otherRetryPolicy = retryPolicy orOnResult {
  case str: String if str.trim isEmpty => rejectResult { keepRetryingSilently }
  case str: String if str.length > maxStringLength => rejectResult
}
```

<a name="error-classifiers"></a>

### Error Classifiers

Errors that occur during a retry attempt can be classified as `Fatal`, `Recoverable` or `SilentlyRecoverable`. `Fatal` errors will interrupt a retry operation and cause it to immediately fail. `Recoverable` errors will be logged or suppressed so that the retry operation can continue. `SilentlyRecoverable` errors will be suppressed without being logged so that the retry operation can continue. Error classifications are defined in [`atmos.ErrorClassification`](http://zman.io/atmos/api/#atmos.ErrorClassification).

Error classifiers are simply implementations of `PartialFunction` that map instances of `Throwable` to the desired error classification. In situations where a classifier is not defined for a particular error, `scala.util.control.NonFatal` is used to classify errors as `Fatal` or `Recoverable`. The appropriate partial function type is defined as [`atmos.ErrorClassifier`](http://zman.io/atmos/api/#atmos.ErrorClassifier) and includes a factory in the companion object.

Error classifiers are configured by calling `onError` to replace the classifier on an existing retry policy, or by using `orOnError` to chain an error classifier to the one that a retry policy already contains:

```scala
import atmos.dsl._

// Stop retrying after any illegal argument exception.
implicit val retryPolicy = retryForever onError { case _: IllegalArgumentException => stopRetrying }

// Extend the above policy by loggging all illegal state exceptions but supressing logging for all other runtime exceptions.
val otherRetryPolicy = retryPolicy orOnError {
  case _: IllegalStateException => keepRetrying
  case _: RuntimeException => keepRetryingSilently
}
```

<a name="retrying-synchronously"></a>

### Retrying Synchronously

To retry synchronously you pass a block of code to the `retry()` method and that block is repeatedly executed until it completes successfully or ultimately fails in accordance with your policy. If a block completes successfully then the value the block evaluates to becomes the return value of `retry()`. If a block fails to complete successfully, meaning it was interrupted by a fatal error or had to abort after too many attempts, then the most recently thrown exception is thrown from `retry()`.

Typically, a retry policy is declared as an implicit variable and the `retry()` method from the DSL is used to execute a synchronous retry operation. However, if you have multiple policies in the same scope (or if you want to avoid using implicit parameters) you can also call `retry()` directly on the policy object:

```scala
import atmos.dsl._

implicit val policy = retryForever

// The following two statements are equivalent:
retry() { doSomething() }
policy.retry() { doSomething() }
```

When calling `retry()` you have the option of giving the operation a name that is included in any log messages:

```scala
import atmos.dsl._

implicit val policy = retryForever

// The following two statements will have a custom operation name in log messages:
retry("Doing something") { doSomething() }
val result1 = retry(Some("Getting something")) { getSomething() }

// The following two statements will have a generic operation name in log messages:
policy.retry() { doSomethingMysterious() }
val result2 = policy.retry(None) { getSomethingMysterious() }
```

You may optionally define an implicit `rummage.Clock`, from the [rummage](http://zman.io/rummage) project, at the point the retry operation is invoked. This is the component responsible for blocking the calling thread until a backoff duration expires. By default, timing is controlled by a singular, global daemon thread. It is unlikely that you will need to provide a custom clock outside of testing.

It is important to note that synchronous retry operations will block the calling thread while waiting for a backoff duration to expire. Use synchronous retries carefully in situations where you do not control the calling thread.

<a name="retrying-asynchronously"></a>

### Retrying Asynchronously

Atmos supports asynchronous retries using Scala Futures. Asynchronous retries are much more involved than their single-threaded cousins, so care must be taken to understand the retry execution model.

To retry asynchronously you call the `retryAsync()` method and pass it a block of code that evaluates to a `scala.concurrent.Future`. A single asyncnhronous attempt consists of executing the block *and* evaluating the outcome of the resulting future. If either the block or the future fails then the attempt fails and normal retry behavior takes over. The `retryAsync()` method returns a future that tracks the entire retry operation regardless of how many attempts are made. If any attempt succeeds then the returned future succeeds with the same value, if the operation fails then the returned future fails with the last reported exception.

When retrying asynchronously, certain additional dependencies must be specified:

 - There must be an implicit `scala.concurrent.ExecutionContext` available at the point the retry operation is invoked. This execution context is where the block provided to `retryAsync()` will be executed during subsequent retries. This can typically be the same context used to execute your futures (if applicable).
 - You may optionally define an implicit `rummage.Clock`, from the [rummage](http://zman.io/rummage) project, at the point the retry operation is invoked. This is the component responsible for providing non-blocking, asynchronous callbacks based on when a backoff duration expires. By default, timing is controlled by a singular, global daemon thread. It is unlikely that you will need to provide a custom clock outside of testing unless you are working with [actors](#retrying-with-actors).
 - The atmos DSL provides support for limiting the amount of time that a future is allowed to complete by providing a `withDeadline` method on `scala.concurrent.Future`.

Asynchronous retries support the same operations as the synchronous form: you may optionally provide an operation name and you can either call this method via the DSL with an implicit retry policy or directly on the retry policy itself.

```scala
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import atmos.dsl._

implicit val policy = retryForever

// The following two statements will have a custom operation name in log messages:
retryAsync("Doing something in the future") { Future { doSomething() } }
val futureResult = retryAsync(Some("Getting something in the future")) { Future { getSomething() } }

// The following two statements will have a generic operation name in log messages:
retryAsync() { Future { doSomethingMysterious() } }
val futureResult = policy.retryAsync(None) { Future { getSomethingInTheFuture() } }

// It is possible to limit the amount of time in which a single retry attempt can complete.
retryAsync() { Future { doSomethingMysterious() } withDeadline 20.seconds }
val futureResult = policy.retryAsync() { Future { doSomethingMysterious() } withDeadline 20.seconds }

// It is possible to limit the amount of time in which an entire retry operation can complete.
retryAsync() { Future { doSomethingMysterious() } } withDeadline 20.seconds
val futureResult = policy.retryAsync() { Future { doSomethingMysterious() } } withDeadline 20.seconds
```

<a name="retrying-with-actors"></a>

### Retrying with Actors

The atmos library has built-in support for [Akka](http://akka.io/), specifically for retrying asynchronously when using the ask pattern. To use this library with actors there are only a couple extra steps involved beyond what is described in [Retrying Asynchronously](#retrying-asynchronously) above.

First, you will want to make sure you have an implicit instance of `rummage.Clock` from the [rummage](http://zman.io/rummage) project in scope, this will make sure that your actor system is the one responsible for scheduling asynchronous backoff timers. This can be accomplished by importing `rummage.AkkaClocks._` inside an actor or by creating an implicit `rummage.AkkaClock` yourself. Second, you'll want to make sure and use Akka logging support to keep your entire retry operation non-blocking.

```scala
import scala.concurrent.duration._
import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import rummage.AkkaClock._
import atmos.dsl._
import AkkaSupport._

val system: ActorSystem = ???
val actor: ActorRef = ???
val otherActor: ActorRef = ???

implicit val policy = retryForever monitorWith Logging(system, this.getClass)
implicit val context = system.dispatcher
implicit val clock = AkkaClock(system.scheduler)
implicit val timeout = Timeout(2 seconds)

retryAsync("Ask an actor over and over") { actor ? "Hello!" } pipeTo otherActor
```

<a name="building-and-testing"></a>

## Building and Testing

Atmos uses [SBT](http://www.scala-sbt.org/), so kicking off a build or running test cases is nice and straightforward.

```sh
# Clone the repository:
git clone git@github.com:zmanio/atmos.git
cd atmos

# Build and package a jar file:
sbt package

# Run the test suite
sbt test

# Generate test coverage reports:
sbt scoverage:test
```

