# Snowplow CLA bot

[![License][license-image]][license]
[![Build Status](https://travis-ci.org/snowplow-incubator/cla-bot.svg?branch=master)](https://travis-ci.org/snowplow-incubator/cla-bot)

Snowplow CLA bot is a web server which handles GitHub webhook events to check whether
PR authors have signed the CLA. It uses Google Sheets as a data source.

### Webhook setup

In your repository, go to `Settings -> Webhooks`, then choose `Add webhook`.
In the *Payload URL* field type the URL address of this bot with `/webhook` endpoint.
Example: `https://example.com/webhook`.

As a content type choose `application/json`. You can either choose the
`Send me everything` option or manually select individuals events to send. You need to
select at least *Pull requests* and *Issue comments*.

### Google Sheets setup

The sheets must contain columns with each row containing a GitHub login of the users
that have signed the CLA. You can configure which columns the bot should look at.

### Configuration

Once the setup is done, we can fill our configuration file.
An example configuration is shown here:

```
port = 8080
host = localhost

github {
  token = 1234567890abcdefghijk
}

gsheets {
  clientId     = GOOGLE_SHEETS_CLIENT_ID
  clientSecret = GOOGLE_SHEETS_CLIENT_SECRET
  accessToken  = GOOGLE_ACCESS_TOKEN
  refreshToken = GOOGLE_REFRESH_TOKEN
}

cla {
  individualCLA {
    # id of the spreadsheet (the one from the spreadsheet URL)
    spreadsheetId = GOOGLE_SPREADSHEET_ID
    sheetName     = GOOGLE_SPREADSHEET_NAME
    # columns containing GitHub logins
    columns        = [ A ]
  }
  corporateCLA {
    # id of the spreadsheet (the one from the spreadsheet URL)
    spreadsheetId = GOOGLE_SPREADSHEET_ID
    sheetName     = GOOGLE_SPREADSHEET_NAME
    # columns containing GitHub logins
    columns        = [ A ]
  }
  # list of GitHub logins which do not require signing the CLA
  peopleToIgnore = [ "scala-steward" ]
}
```

### Running

You can run the project using Docker, through a helper script:
```bash
sbt docker:publishLocal
sh target/docker/stage/opt/docker/bin/cla-bot -Dconfig.fle=application.conf
```

Or run the jar directly:

```bash
sbt assembly
java -Dconfig.file=application.conf \
  -jar target/scala-2.12/cla-bot-0.1.0.jar
```

### How the bot algorithm works

##### Pull Request is opened
- If user submitting the PR is a collaborator (this includes members of the organization),
  the bot ignores the PR.

- If the user is not a collaborator
  - and is in the `peopleToIgnore` list, or
  - and has signed the CLA, the bot adds a `cla:yes` label

- If the user is not a collaborator, is not in the `peopleToIgnore` list and has not signed
  the CLA, the bot adds a `cla:no` label and posts a comment reminding the user to sign the CLA.

  The bot then listens to incoming comments in the PR. If the author of the
  comment is also the author of the PR ("pinging"), then the bot checks the
  CLA again. If the CLA is now signed, the bot posts a comment with a thank you message.
  Otherwise it ignores the comment.

## Copyright and license

The Snowplow CLA Bot is copyright 2018-2020 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[webhooks]: https://developer.github.com/webhooks/
[log4j]: https://docs.oracle.com/cd/E29578_01/webhelp/cas_webcrawler/src/cwcg_config_log4j_file.html
