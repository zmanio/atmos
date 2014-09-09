A [Scala](http://www.scala-lang.org/) library for describing retry-on-failure behavior using a concise, literate [embedded DSL](http://c2.com/cgi/wiki?EmbeddedDomainSpecificLanguage).

There are places in most modern software where small, intermittent errors can occur and disrupt the normal flow of execution. Traditionally, a *retry loop* is used in these situations, with constraints on the number of attempts that can be made and how certain errors are handled. A naive retry loop that would make up to three attempts, waiting 100 milliseconds between each attempt, could look like this:

```scala
def doSomethingUntilItWorks(): String = {
  val maxAttempts = 3
  val backoff = 100 millis
  var attempts = 0
  while (true) {
    attempts += 1
    try {
      return doSomethingThatMightFail()
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
```

Retry loops like the one above bring a number of issues with them:

 - They obscure the actual work that the program is trying to do (the lone call to `doSomethingThatMightFail()` above).

 - They are convoluted and tend to contain more and more mutable state the more complex they get, making them hard to reason about and resistant to change.

 - They are difficult and tedious to test, possibly leading to undiscovered bugs in the code base.

With the atmos library, a *retry policy* can be described using a minimalistic DSL, making unreadable retry loops a thing of the past. The above example can be described using atmos like so:

```scala
import atmos.dsl._

implicit val retryPolicy = retryFor { 3 attempts } using constantBackoff { 100 millis } monitorWith System.out onError {
  case _: SomeImportantException => stopRetrying
}

val result = retry() { doSomethingThatMightFail() }
```

In addition to making retry behavior easy to understand, atmos provides the ability to customize the policies that control [loop termination](#termination-policies), [backoff calculation](#backoff-policies), [error handling](#error-classifiers) and [event monitoring](#event-monitors), as well as supporting both [synchronous](#retrying-synchronously) and [asynchronous](#retrying-asynchronously) styles of programming. See the [user guide](#using-the-library) below for information about the wide array of customization options this library supports.

[code](https://github.com/zmanio/atmos) - [api](http://zman.io/atmos/api/#atmos.package) - [history](changelog/)

[![Build Status](https://travis-ci.org/zmanio/atmos.png?branch=master)](https://travis-ci.org/zmanio/atmos) [![Coverage Status](https://coveralls.io/repos/zmanio/atmos/badge.png)](https://coveralls.io/r/zmanio/atmos)

## Table of Contents

[Getting Started](#getting-started)

[Using the Library](#using-the-library)

 - [Termination Policies](#termination-policies)

 - [Backoff Policies](#backoff-policies)

 - [Error Classifiers](#error-classifiers)

 - [Event Monitors](#event-monitors)

 - [Retrying Synchronously](#retrying-synchronously)

 - [Retrying Asynchronously](#retrying-asynchronously)

 - [Example Retry Policies](#example-retry-policies)

[Building and Testing](#building-and-testing)

## Getting Started

Prerequisites:

 - [Scala](http://scala-lang.org/) 2.10.x

 - [SBT](http://www.scala-sbt.org/) or a similar build tool.

<!---
To use atmos in your project simply add one line to your SBT configuration:

```scala
libraryDependencies += "io.zman" %% "atmos" % "2.0"
```

[Instructions for other build systems](http://mvnrepository.com/artifact/io.zman/atmos_2.10/2.0).
-->

## Using the Library

The atmos library divides the definition of a retry policy into four parts:

 - Termination policies enforce an upper bound on the number of retry attempts that are made.

 - Backoff policies calculate the delay that is inserted before subsequent retry attempts.

 - Error classifiers define the strategy used to determine if an error prevents further attempts.

 - Event monitors are notified of events that occur while performing a retry operation.

Using the naive retry loop from above, we can classify its behavior according to the four elements of a retry policy:


```scala
while (true) {
  attempts += 1                        // Termination policy
  try {
    return doSomethingThatMightFail()
  } catch {
    case e: SomeImportantException =>  // Error classifier
      println("interrupted")           // Event monitor
      throw e
    case NonFatal(e) =>                // Error classifier
      if (attempts >= maxAttempts) {   // Termination policy
        println("aborting")            // Event monitor
        throw e
      }
      println("retrying")              // Event monitor
      Thread.sleep(backoff toMillis)   // Backoff policy
    case e =>
      println("interrupted")           // Event monitor
      throw e
  }
}
```

Atmos decomposes the traditional retry loop into these four, independent strategies and allows you to easily recombine them in whatever fashion you see fit.

### Termination Policies

### Backoff Policies

### Error Classifiers

### Event Monitors

### Retrying Synchronously

### Retrying Asynchronously

### Example Retry Policies

## Building and Testing