# Snowplow CLA bot

[![Build Status](https://travis-ci.com/snowplow-incubator/cla-bot.svg?token=rA744zFX5YFUNdyp1U6x&branch=master)](https://travis-ci.com/snowplow-incubator/cla-bot)

### Webhook setup

The endpoint for the GitHub webhook is `/webhook`.

### Configuration

Once all this setup is done, we can fill up our configuration file.
An example configuration is shown here:

```
port = 8080

github.token    = 1234567890abcdefghijk
github.bot-name = "snowplow-cla-bot"

gsheets.client-id      = foobar.apps.googleusercontent.com
gsheets.client-secret  = YOUR_GSHEETS_SECRET
gsheets.access-token   = YOUR_GSHEETS_ACCESS_TOKEN
gsheets.refresh-token  = YOUR_GSHEETS_REFRESH_TOKEN
gsheets.spreadsheet-id = SPREADSHEET_ID
gsheets.sheet-name     = SHEET_NAME
gsheets.column         = A
```

### Logging

SRS uses log4j to log messages and possible errors. Refer to [the documentation][log4j] to write a
proper log4j file.

### Running

You just need to run:

```bash
sbt assembly
java -Dconfig.file=application.conf \
  -Dlog4j.configuration=log4j.properties \
  -jar target/scala-2.12/schema-registry-sync-0.1.0.jar
```

[webhooks]: https://developer.github.com/webhooks/
[log4j]: https://docs.oracle.com/cd/E29578_01/webhelp/cas_webcrawler/src/cwcg_config_log4j_file.html
