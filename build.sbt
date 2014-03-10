import de.johoop.jacoco4sbt._
import JacocoPlugin._

//
// Basic project information.
//

organization := "atmos"

name := "atmos"

version := "1.3"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.1.RC1" % "test"
)

jacoco.settings

//
// Documentation site generation.
//

site.settings

site.includeScaladoc(".")

ghpages.settings

git.remoteRepo := "git@github.com:lpryor/atmos.git"
