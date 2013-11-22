Atmos: Scala Utilities for the Cloud
------------------------------------

Atmos is a Scala library for solving common problems encountered while working in the cloud.

Currently, Atmos provides two packages:

 - [atmos.retries](http://lpryor.github.io/atmos/#atmos.retries.package) - A concise library for implementing retry-on-failure behavior.
 - [atmos.utils](http://lpryor.github.io/atmos/#atmos.utils.package) - Miscellaneous small, self-contained tools.
    
API Documentation is [available online](http://lpryor.github.io/atmos/#atmos.package).

See [CHANGELOG.md](CHANGELOG.md) for the project history.

### Building ###

You need SBT 0.13 or higher.

    # Compile and package jars.
    sbt package

    # Generate API documentation.
    sbt doc

### Target platform ###

* Scala 2.10+
* JVM 1.5+

### License ###

Atmos is is licensed under the terms of the
[Apache Software License v2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

