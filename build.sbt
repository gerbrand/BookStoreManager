name := "tiny-store-manager"

ThisBuild / scalaVersion     := "2.13.7"
ThisBuild / versionScheme    := Some("semver-spec")
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "nl.software-creation"
ThisBuild / organizationName := "software-creation"

description := "Tiny Store Manager"

resolvers += "Sonatype OSS S01 Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

resolvers += "Sonatype OSS S01 Releases" at "https://s01.oss.sonatype.org/content/repositories/releases"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "bolcom-bintray" at "https://dl.bintray.com/pvdissel/bol-com-releases")

lazy val akkaHttpVersion = "10.2.5-M1"
lazy val akkaVersion    = "2.6.15"

libraryDependencies += "com.h2database" % "h2" % "1.4.199"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-actor-typed"         % akkaVersion,
  "com.typesafe.akka"  %% "akka-stream"              % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json"      % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-slf4j"               % akkaVersion,
  "ch.qos.logback"     % "logback-classic"           % "1.2.3",
  "org.threeten"       % "threeten-extra"            % "1.5.0",
  "com.typesafe.akka"  %% "akka-http-testkit"        % akkaHttpVersion % Test,
  "com.typesafe.akka"  %% "akka-actor-testkit-typed" % akkaVersion     % Test,
  "org.scalatest"      %% "scalatest"                % "3.0.8"         % Test,
  "org.scalacheck"     %% "scalacheck"               % "1.14.2"        % Test
)

libraryDependencies += "nl.software-creation" %% "woocommerce-scala-akka-client" % "0.1.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.3.6",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.apache.poi" % "poi-ooxml" % "4.1.0",
  "com.bol.openapi" % "openapi-java-client" % "4.1.0"
)

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "1.0.8"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-akkastream" % "1.0.8"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-play-json-compat" % "1.0.8-play28"
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "1.0.8-play28"

// CLI Library
libraryDependencies += "org.rogach" %% "scallop" % "4.1.0"

libraryDependencies ++= Seq(
  "com.google.api-client" % "google-api-client" % "1.30.9",
  "com.google.apis" % "google-api-services-books" % "v1-rev20200204-1.30.9",
)

lazy val root = (project in file("."))
   .settings(
    name := "tiny-store-manager",
    mainClass := Some("cli.ConvertBolCom"))
