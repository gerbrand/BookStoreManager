name := "tiny-store-manager"

description := "Tiny Store Manager"

version := "0.1"

scalaVersion := "2.13.1"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "bolcom-bintray" at "https://dl.bintray.com/pvdissel/bol-com-releases")

lazy val akkaHttpVersion = "10.1.12"
lazy val akkaVersion    = "2.6.5"
lazy val circeVersion = "0.13.0"

val jsonDependencies: List[ModuleID] = List(
  "io.circe"                          %% "circe-core"                 % circeVersion,
  "io.circe"                          %% "circe-generic"              % circeVersion,
  "io.circe"                          %% "circe-parser"               % circeVersion,
  "io.circe"                          %% "circe-literal"              % circeVersion,
)

libraryDependencies += "com.h2database" % "h2" % "1.4.199"

libraryDependencies ++= Seq(
  "com.typesafe.akka"  %% "akka-http"                % akkaHttpVersion,
  "com.typesafe.akka"  %% "akka-actor-typed"         % akkaVersion,
  "de.heikoseeberger"  %% "akka-http-circe"          % "1.29.1",
  "com.typesafe.akka"  %% "akka-stream"              % akkaVersion,
  "com.typesafe.akka"  %% "akka-slf4j"               % akkaVersion,
  "ch.qos.logback"     % "logback-classic"           % "1.2.3",
  "org.threeten"       % "threeten-extra"            % "1.5.0",
  "com.typesafe.akka"  %% "akka-http-testkit"        % akkaHttpVersion % Test,
  "com.typesafe.akka"  %% "akka-actor-testkit-typed" % akkaVersion     % Test,
  "org.scalatest"      %% "scalatest"                % "3.0.8"         % Test,
  "org.scalacheck"     %% "scalacheck"               % "1.14.2"        % Test
) ++ jsonDependencies

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.3.6",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  "org.apache.poi" % "poi-ooxml" % "4.1.0",
  "com.bol.openapi" % "openapi-java-client" % "4.1.0"
)

libraryDependencies ++= Seq(
  "com.google.api-client" % "google-api-client" % "1.30.9",
  "com.google.apis" % "google-api-services-books" % "v1-rev20200204-1.30.9",
)

lazy val root = (project in file("."))
   .settings(
    name := "tiny-store-manager",
    mainClass := Some("cli.ConvertBolCom"))

