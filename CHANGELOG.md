1.2 (2014-02-??)
================
Enhancements to the backoff policies in `atmos.retries`:

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