import SonatypeKeys._

//
// Basic project information.
//

name := "atmos"

version := "2.1"

description := "minimalist retry-on-failure behavior for scala"

homepage := Some(url("http://zman.io/atmos/"))

startYear := Some(2013)

organization := "io.zman"

organizationName := "zman.io"

organizationHomepage := Some(url("http://zman.io/"))

scalaVersion := "2.11.6"

crossScalaVersions := Seq("2.10.4", "2.11.6")

libraryDependencies ++= Seq(
  "io.zman" %% "rummage" % "1.3",
  "com.typesafe.akka" %% "akka-actor" % "2.3.2" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
)

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

//
// Publishing to Sonatype
//

sonatypeSettings
  
pomExtra := (
  <scm>
    <url>git@github.com:zmanio/atmos.git</url>
    <connection>scm:git:git@github.com:zmanio/atmos.git</connection>
    <developerConnection>scm:git:git@github.com:zmanio/atmos.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>lonnie</id>
      <name>Lonnie Pryor</name>
      <url>http://zman.io</url>
    </developer>
  </developers>
)
