import SiteKeys._
import GhPagesKeys._
import SonatypeKeys._

//
// Basic project information.
//

name := "atmos"

version := "2.0-SNAPSHOT"

description := "A concise Scala library for implementing retry-on-failure behavior."

homepage := Some(url("http://zman.io/atmos/"))

startYear := Some(2013)

organization := "io.zman"

organizationName := "zman.io"

organizationHomepage := Some(url("http://zman.io/"))

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "io.zman" %% "rummage" % "1.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.0" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.1.RC1" % "test"
)

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

ScoverageSbtPlugin.instrumentSettings

CoverallsPlugin.coverallsSettings

//
// Documentation site generation.
//

site.settings

includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.md" | "*.yml"

site.includeScaladoc("api")

ghpages.settings

ghpagesNoJekyll := false

git.remoteRepo := "git@github.com:zmanio/atmos.git"

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