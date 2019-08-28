name := "tiny-store-manager"

description := "Tiny Store Manager"

version := "0.1"

scalaVersion := "2.13.0"

TwirlKeys.templateImports += "com.example.user.User"

libraryDependencies += guice
libraryDependencies += "com.h2database" % "h2" % "1.4.199"

// Automatic database migration available in testing
fork in Test := true
val playVersion = play.core.PlayVersion.current
libraryDependencies += "org.flywaydb" % "flyway-core" % "5.1.1"
libraryDependencies += "com.typesafe.play" %% "play-ahc-ws" % playVersion % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

lazy val flyway = (project in file("modules/flyway"))
  .enablePlugins(FlywayPlugin)

lazy val api = (project in file("modules/api"))
  .enablePlugins(JavaAppPackaging)
  .settings(Common.projectSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      "com.github.tototoshi" %% "scala-csv" % "1.3.6",
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.apache.poi" % "poi-ooxml" % "4.1.0",
      "com.bol.openapi" % "openapi-java-client" % "4.1.0",
      "com.google.api-client" % "google-api-client" % "1.30.2",
    )
  )


lazy val slick = (project in file("modules/slick"))
  .settings(Common.projectSettings)
  .aggregate(api)
  .dependsOn(api)

lazy val cli = (project in (file("cli")))
  .enablePlugins(JavaAppPackaging)
  .settings(Common.projectSettings)
  .settings(
    name := "cli",
    mainClass := Some("cli.ConvertBolCom"))
  .dependsOn(slick)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(cli)
  .dependsOn(slick)

