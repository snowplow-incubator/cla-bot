import sbt._
import sbt.Keys._

import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.Keys._

object BuildSettings {

  lazy val baseSettings = Seq(
    Compile / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    Test / console / scalacOptions ~= {
      _.filterNot(Set("-Ywarn-unused-import"))
    },
    evictionErrorLevel := sbt.util.Level.Warn
  )
  
  lazy val dockerSettings = Seq(
    dockerBaseImage := "openjdk:8u171-jre-alpine",
    dockerExposedPorts := Seq(8080)
  )


}
