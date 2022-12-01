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
package clabot

import cats.data.NonEmptyList

object config {

  final case class GithubConfig(token: String)

  final case class GoogleSheet(
                                spreadsheetId: String,
                                sheetName: String,
                                columns: NonEmptyList[String]
                              ) {
    def range: List[String] = columns.map(col => s"$sheetName!$col:$col").toList
  }

  final case class CLAConfig(
                              internalCLA: GoogleSheet,
                              individualCLA: GoogleSheet,
                              corporateCLA: GoogleSheet,
                              peopleToIgnore: List[String]
                            )

  final case class CLABotConfig(
                                 port: Int,
                                 host: String,
                                 github: GithubConfig,
                                 oathCredPath: String,
                                 cla: CLAConfig
                               )
}
