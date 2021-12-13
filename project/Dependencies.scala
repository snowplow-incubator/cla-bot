import sbt._
import sbt.Keys._

object Dependencies {
  object V {
    lazy val catsEffect = "3.2.9"
    lazy val refined = "0.9.27"
    lazy val atto = "0.9.5"
    lazy val scalaUri = "3.6.0"
    lazy val scalacheck = "1.15.4"
    lazy val scalatest = "3.0.5"

    lazy val http4s = "0.23.6"
    lazy val circe = "0.14.1"
    lazy val circeConfig = "0.8.0"
    lazy val github4s = "0.30.0"
    lazy val logback = "1.2.7"
    lazy val specs2 = "4.13.0"
    lazy val mockito = "0.3.0"
    lazy val oauthVersion = "1.13.0"
    lazy val oauthClientClient = "1.33.3"
    lazy val gapiVersion = "v4-rev20220620-1.32.1"
  }

  lazy val All = Seq(
    "org.http4s" %% "http4s-dsl" % V.http4s,
    "org.http4s" %% "http4s-blaze-server" % V.http4s,
    "org.http4s" %% "http4s-blaze-client" % V.http4s,
    "org.http4s" %% "http4s-circe" % V.http4s,

    "io.circe" %% "circe-generic" % V.circe,
    "io.circe" %% "circe-config" % V.circeConfig,

    "com.google.auth" % "google-auth-library-oauth2-http" % V.oauthVersion,
    "com.google.oauth-client" % "google-oauth-client" % V.oauthClientClient,
    "com.google.apis" % "google-api-services-sheets" % V.gapiVersion,

    "com.47deg" %% "github4s" % V.github4s,
    "ch.qos.logback" % "logback-classic" % V.logback,

    "org.specs2" %% "specs2-core" % V.specs2 % Test,
    "org.specs2" %% "specs2-mock" % V.specs2 % Test
  )
}
