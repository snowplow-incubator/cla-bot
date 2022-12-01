/*
 * Copyright (c) 2018-2022 Snowplow Analytics Ltd. All rights reserved.
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

lazy val claBot = project.in(file("clabot"))
  .settings(Seq(
    organization := "com.snowplowanalytics",
    name := "cla-bot",
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
  ))
  .settings(BuildSettings.baseSettings)
  .settings(BuildSettings.assemblySettings)
  .settings(BuildSettings.dynVerSettings)
  .settings(libraryDependencies := Dependencies.All)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(JavaAppPackaging, SnowplowDockerPlugin)
