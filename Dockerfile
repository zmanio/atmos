# Atmos
#
# Version 1.3

# install Scala 2.10.3, Java 7 update 45, and sbt 0.13.1 on top of Ubuntu 12.04 LTS.
FROM lukasz/docker-scala
MAINTAINER Veronica Ray, veronica.l.ray@gmail.com

# Compile and package jars.
RUN sbt package

# Generate API documentation.
RUN sbt doc