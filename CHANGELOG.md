1.3 (2014-??-??)
================
Reworked `atmos.retries.EventMonitor.PrintEvents`:

 - Converted `PrintEvents` into a trait and extracted two concrete subtypes, `PrintEventsWithStream` and `PrintEventsWithWriter`.
 
 - Deprecated the use of booleans to signal whether a stack trace should be printed for a particular event. The booleans are replaced by `PrintEvents.PrintAction`, a `sealed trait` / `case object` enumeration capable of representing any number of printing strategies.
 
 - Modified the retry DSL to support concise configuration of print actions on event monitors derived from print streams and print writers.
 
Reworked `atmos.retries.EventMonitor.LogEvents`:

 - Converted `LogEvents` into a trait and extracted the concrete
subtype `LogEventsWithJava`.
 
 - Deprecated the use of log levels to describe behavior when called with a particular event. The levels are replaced by
`LogEvents.LogAction`, a `sealed trait` / `case object` enumeration
capable of representing any number of logging strategies.

1.2 (2014-02-18)
================
Enhancements to the backoff policies in `atmos.retries`:

 - Added `BackoffPolicy.Selected`, a policy that selects another backoff policy based on the most recent exception.
 
 - Added `BackoffPolicy.Randomized`, a policy that randomizes the result of another backoff policy.

 - Removed the previous backoff value from the list of items provided to a `BackoffPolicy` when calculating a backoff and replaced it with the most recently thrown exception.
 
Enhancements to the termination policies in `atmos.retries`:

 - Added `TerminationPolicy.ImmediatelyTerminate`, a policy that never retries.

Internal project changes:

  - Removed obsolete JUnit dependency.

  - Changed to FlatSpec as opposed to using FunSpec.

1.1 (2013-12-17)
================
Added support for SLF4J monitoring to `atmos.retries`.

1.0 (2013-11-21)
================
Initial release.