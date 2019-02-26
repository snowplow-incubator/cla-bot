/*
 * Copyright (c) 2018-2018 Snowplow Analytics Ltd. All rights reserved.
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

import gsheets4s.model.Credentials

object config {

  final case class GithubConfig(token: String)

  sealed trait GoogleSheet {
    def spreadsheetId: String
    def sheetName: String
  }

  final case class IndividualCLAConfig(
    override val spreadsheetId: String,
    override val sheetName: String,
    column: String
  ) extends GoogleSheet

  final case class CLAConfig(
    individualCLA: IndividualCLAConfig,
    peopleToIgnore: List[String]
  )

  final case class GSheetsConfig(
    accessToken: String,
    refreshToken: String,
    clientId: String,
    clientSecret: String
  ) {
    def toCredentials: Credentials = Credentials(accessToken, refreshToken, clientId, clientSecret)
  }

  final case class CLABotConfig(
    port: Int,
    host: String,
    github: GithubConfig,
    gsheets: GSheetsConfig,
    cla: CLAConfig
  )
}
