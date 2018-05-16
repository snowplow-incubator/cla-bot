organization in ThisBuild := "com.snowplowanalytics"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  scalaVersion := "2.12.6",
  version := "0.1.0-SNAPSHOT"
)

lazy val fs2Version = "0.10.4"
lazy val circeVersion = "0.9.3"
lazy val github4sVersion = "0.18.4"
lazy val gsheeets4sVersion = "0.1.0-SNAPSHOT"
lazy val circeConfigVersion = "0.4.1"
lazy val scalajHttpVersion = "2.4.0"
lazy val json4sVersion = "3.5.3"
lazy val awsSdkVersion = "1.11.301"
lazy val scalatestVersion = "3.0.5"
lazy val mockitoVersion = "2.17.0"

lazy val claBot = project.in(file("."))
  .settings(name := "cla-bot")
  .settings(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % fs2Version,
      "com.47deg" %% "github4s" % github4sVersion,
      "com.github.benfradet" %% "gsheets4s" % gsheeets4sVersion,
      "io.circe" %% "circe-config" % circeConfigVersion,
      "org.scalaj" %% "scalaj-http" % scalajHttpVersion,
      "org.json4s" %% "json4s-native" % json4sVersion,
      "com.amazonaws" % "aws-java-sdk-sqs" % awsSdkVersion
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion) ++ Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion,
      "org.mockito" % "mockito-core" % mockitoVersion
    ).map(_ % "test")
  )
