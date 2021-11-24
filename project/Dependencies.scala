import sbt._
import sbt.Keys._

object Dependencies {
  object V {
    lazy val catsEffect = "1.4.0"
    lazy val refined = "0.9.2"
    lazy val atto = "0.6.3"
    lazy val hammock = "0.9.0"
    lazy val scalaUri = "3.6.0"
    lazy val scalacheck = "1.14.0"
    lazy val scalatest = "3.0.5"

    lazy val http4s = "0.20.9"
    lazy val circe = "0.11.1"
    lazy val circeConfig = "0.6.1"
    lazy val github4s = "0.20.1"
    lazy val logback = "1.2.3"
    lazy val specs2 = "4.6.0"
    lazy val mockito = "0.3.0"
  }

  lazy val Gsheets4s = Seq(
    "org.typelevel"   %% "cats-effect"         % V.catsEffect,
    "eu.timepit"      %% "refined"             % V.refined,
    "io.lemonlabs"    %% "scala-uri"           % V.scalaUri,

    "io.circe"        %% "circe-core"          % V.circe,
    "io.circe"        %% "circe-generic"       % V.circe,
    "io.circe"        %% "circe-parser"        % V.circe,

    "org.tpolecat"    %% "atto-core"           % V.atto,
    "org.tpolecat"    %% "atto-refined"        % V.atto,

    "com.pepegar"     %% "hammock-core"        % V.hammock,
    "com.pepegar"     %% "hammock-circe"       % V.hammock,
    "com.pepegar"     %% "hammock-apache-http" % V.hammock,

    "org.specs2"      %% "specs2-core"         % V.specs2     % Test,
    "org.specs2"      %% "specs2-mock"         % V.specs2     % Test,
    "org.scalacheck"  %% "scalacheck"          % V.scalacheck % Test,
    "eu.timepit"      %% "refined-scalacheck"  % V.refined    % Test
  )

  lazy val All = Seq(
    "org.http4s"              %% "http4s-dsl"           % V.http4s,
    "org.http4s"              %% "http4s-blaze-server"  % V.http4s,
    "org.http4s"              %% "http4s-circe"         % V.http4s,

    "io.circe"                %% "circe-generic"        % V.circe,
    "io.circe"                %% "circe-config"         % V.circeConfig,

    "com.47deg"               %% "github4s"             % V.github4s,
    "com.47deg"               %% "github4s-cats-effect" % V.github4s,
    "ch.qos.logback"          %  "logback-classic"      % V.logback,

    "org.specs2"              %% "specs2-core"          % V.specs2     % Test,
    "org.specs2"              %% "specs2-mock"          % V.specs2     % Test
  )
}
