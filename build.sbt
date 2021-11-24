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

lazy val gsheets4s = project.in(file("gsheets4s"))
  .settings(BuildSettings.baseSettings)
  .settings(libraryDependencies := Dependencies.Gsheets4s)

lazy val claBot = project.in(file("clabot"))
  .settings(Seq(
    organization := "com.snowplowanalytics",
    version := "0.2.0",
    name := "cla-bot",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
  ))
  .settings(BuildSettings.baseSettings)
  .settings(BuildSettings.dockerSettings)
  .settings(libraryDependencies := Dependencies.All)
  .dependsOn(gsheets4s)
  .enablePlugins(JavaServerAppPackaging)
