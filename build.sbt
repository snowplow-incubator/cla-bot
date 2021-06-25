/*
 * Copyright (c) 2018-2020 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import com.typesafe.sbt.packager.docker._

lazy val baseSettings = Seq(
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  organization := "com.snowplowanalytics",
  scalaVersion := "2.12.8",
  version := "0.2.0",
  name := "cla-bot",
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "openjdk:8u171-jre-alpine",
  dockerExposedPorts := Seq(8080)
)

lazy val http4sVersion = "0.20.9"
lazy val circeVersion = "0.11.1"
lazy val circeConfigVersion = "0.6.1"
lazy val github4sVersion = "0.20.1"
lazy val gsheeets4sVersion = "0.2.0"
lazy val logbackVersion = "1.2.3"
lazy val specs2Version = "4.12.2"
lazy val mockitoVersion = "0.3.0"

lazy val claBot = project.in(file("."))
  .enablePlugins(JavaServerAppPackaging)
  .settings(baseSettings)
  .settings(dockerSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-blaze-server",
      "org.http4s" %% "http4s-circe",
    ).map(_ % http4sVersion) ++ Seq(
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-config" % circeConfigVersion,
      "com.47deg" %% "github4s-cats-effect" % github4sVersion,
      "com.github.benfradet" %% "gsheets4s" % gsheeets4sVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    ) ++ Seq(
      "org.specs2" %% "specs2-core",
      "org.specs2" %% "specs2-mock",
    ).map(_ % specs2Version % "test")
  )
