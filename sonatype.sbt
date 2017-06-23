
//
// Publishing to Sonatype
//

credentials += Credentials("Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USER", default = ""),
  sys.env.getOrElse("SONATYPE_PASSWORD", default = ""))

// Your profile name of the sonatype account. The default is the same with the organization value
publishMavenStyle := true

publishArtifact in Test := false

sonatypeProfileName := "io.paradoxical"

pomIncludeRepository := { _ => false }

pgpPassphrase := Some(sys.env.getOrElse("GPG_PASSWORD", default = "").toArray)

pomExtra := (
  <scm>
    <url>git@github.com:paradoxical-io/atmos.git</url>
    <connection>scm:git:git@github.com:paradoxical-io/atmos.git</connection>
    <developerConnection>scm:git:git@github.com:paradoxical-io/atmos.git</developerConnection>
  </scm>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>lonnie</id>
        <name>Lonnie Pryor</name>
        <url>http://zman.io</url>
      </developer>
      <developer>
        <id>devshorts</id>
        <name>Anton Kropp</name>
        <url>http://onoffswitch.net</url>
      </developer>
    </developers>
  )