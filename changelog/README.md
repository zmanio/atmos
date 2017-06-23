# 2.2 (2017-06-23) 

- Forked to support scala 2.12. Pulled in rummage namespace to simplify deployment. Dropping 2.10 support

# 2.1 (2015-05-23)

 - Added support for Scala 2.11 in addition to Scala 2.10.
 
 - Added `atmos.ResultClassifier`, `atmos.ResultClassification` and modified the retry logic to support unacceptable
   results. Also added `onResult`, `acceptResult`, `rejectResult` and other supporting classes to the DSL.
 
 - Changed backoff policies from accepting `previousError: Throwable` to `outcome: Try[Any]` in order to support the
   addition of result classifiers. This is a breaking change for any implementations of `BackoffPolicy`, as well as any
   uses of `atmos.backoff.SelectedBackoff` or the DSL method `selectedBackoff`. Uses of other backoff implementations
   or other backoff elements of the DSL will continue to compile normally.
 
 - Changed event monitors from accepting `thrown: Throwable` to `outcome: Try[Any]` in order to support the addition of
   result classifiers. This is a breaking change for any implementations of `EventMonitor` or extensions of the
   classes defined in `atmos.monitor`. Direct usage of the DSL will continue to compile normally.
   
 - Added support for chaining together multiple event monitors in `atmos.events.ChainedEvents` and in the DSL as
   `alsoMonitorWith`.
   
 - Added library and DSL support for fine grained event monitoring to all supported event monitors.
   
 - Added support for chaining together multiple result classifiers and error classifiers in the DSL.
   
 - Moved from using the deprecated `rummage.Timer` to the new `rummage.Clock` for tracking time and implementing synchronous and asynchronous backoffs.
   
 - Included the `rummage.Deadlines` DSL in the atmos DSL, enabling the use of `withDeadline` on arbitrary futures.

# 2.0.1 (2015-04-18)

Rebuilt the 2.0 library with a version of [scoverage](https://github.com/scoverage/sbt-scoverage) that does not add itself to this library's transitive dependency chain.

# 2.0 (2014-09-12)

Moved to the `io.zman` organization namespace and moved from the `atmos.retries` package to the `atmos` package.

In addition to the namespace changes, much of the code was refactored to break up files that had begun to grow far too
large. There were no notable functionality changes in this release.

# 1.3 (2014-03-10)

Reworked `atmos.retries.EventMonitor.PrintEvents`:

 - Converted `PrintEvents` into a trait and extracted two concrete subtypes, `PrintEventsWithStream` and
   `PrintEventsWithWriter`.
 
 - Deprecated the use of booleans to signal whether a stack trace should be printed for a particular event. The
   booleans are replaced by `PrintEvents.PrintAction`, a `sealed trait` / `case object` enumeration capable of
   representing any number of printing strategies.
 
 - Modified the retry DSL to support concise configuration of print actions on event monitors derived from print
   streams and print writers.
 
Reworked `atmos.retries.EventMonitor.LogEvents`:

 - Converted `LogEvents` into a trait and extracted the concrete subtype `LogEventsWithJava`.
 
 - Deprecated the use of log levels to describe behavior when called with a particular event. The levels are replaced
   by `LogEvents.LogAction`, a `sealed trait` / `case object` enumeration capable of representing any number of logging
   strategies.

 - Modified the retry DSL to support concise configuration of log actions on event monitors derived from Java loggers.

Reworked the Slf4j support in `atmos.retries.EventMonitor`:

 - Created `LogEventsWithSlf4j` as a subtype of `LogEvents`.

 - Converted `LogEventsToSlf4j` into a deprecated collection of aliases to aspects of `LogEventsWithSlf4j`.

 - Deprecated the use of log levels to describe behavior when called with a particular event, replacing them with
  `LogEvents.LogAction`, a `sealed trait` / `case object` enumeration capable of representing any number of logging
   strategies.

 - Modified the retry DSL to support concise configuration of log actions on event monitors derived from Slf4j loggers.
 
Added support for asynchronous monitoring with Akka to `atmos.retries`:

 - Created `LogEventsWithAkka` as a subtype of `LogEvents`.

 - Modified the retry DSL to support concise configuration of log actions on event monitors derived from Akka logging
   adapters.

# 1.2 (2014-02-18)

Enhancements to the backoff policies in `atmos.retries`:

 - Added `BackoffPolicy.Selected`, a policy that selects another backoff policy based on the most recent exception.
 
 - Added `BackoffPolicy.Randomized`, a policy that randomizes the result of another backoff policy.

 - Removed the previous backoff value from the list of items provided to a `BackoffPolicy` when calculating a backoff
   and replaced it with the most recently thrown exception.
 
Enhancements to the termination policies in `atmos.retries`:

 - Added `TerminationPolicy.ImmediatelyTerminate`, a policy that never retries.

Internal project changes:

  - Removed obsolete JUnit dependency.

  - Changed to FlatSpec as opposed to using FunSpec.

# 1.1 (2013-12-17)

Added support for SLF4J monitoring to `atmos.retries`.

# 1.0 (2013-11-21)

Initial release.