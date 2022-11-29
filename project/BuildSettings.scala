import sbt._
import sbt.Keys._

import com.typesafe.sbt.packager.Keys._

import sbtdynver.DynVerPlugin.autoImport._

object BuildSettings {

  lazy val baseSettings = Seq(
    scalaVersion := "2.13.9",
    Compile / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    Test / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    evictionErrorLevel := sbt.util.Level.Warn
  )

  /** sbt-assembly settings for building a fat jar */
  import sbtassembly.AssemblyPlugin.autoImport._
  lazy val assemblySettings = Seq(
    assembly / assemblyJarName := { s"${moduleName.value}-${version.value}.jar" },
    assembly / assemblyMergeStrategy := {
      case "AUTHORS" => MergeStrategy.first
      case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
      case x if x.endsWith("native-image.properties") => MergeStrategy.first
      case x if x.endsWith("module-info.class") => MergeStrategy.first
      case x if x.endsWith("reflection-config.json") => MergeStrategy.first
      case x if x.startsWith("codegen-resources") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

  lazy val dynVerSettings = Seq(
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    ThisBuild / dynverSeparator := "-" // to be compatible with docker
  )
}
