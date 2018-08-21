# Snowplow CLA bot

[![Build Status](https://travis-ci.com/snowplow-incubator/cla-bot.svg?token=rA744zFX5YFUNdyp1U6x&branch=master)](https://travis-ci.com/snowplow-incubator/cla-bot)

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

The sheet must contain a column with each row containing a GitHub login of the users 
that have signed the CLA. You can configure which column the bot should look at.

### Configuration

Once all setup is done, we can fill up our configuration file.
An example configuration is shown here:

```
port = 8080

github {
    token = 1234567890abcdefghijk
}

gsheets {
    client-id      = foobar.apps.googleusercontent.com
    client-secret  = YOUR_GSHEETS_SECRET
    access-token   = YOUR_GSHEETS_ACCESS_TOKEN
    refresh-token  = YOUR_GSHEETS_REFRESH_TOKEN
    spreadsheet-id = SPREADSHEET_ID
    sheet-name     = SHEET_NAME
    column         = A
}
```

### Running

You just need to run:

```bash
sbt assembly
java -Dconfig.file=application.conf \
  -jar target/scala-2.12/cla-bot-0.1.0.jar
```

### How does the bot algorithm work

*Note: the bot currently does not handle the Software Grant and Corporate Contributor License Agreement.*

##### Pull Request is opened
- If user submitting the PR is a collaborator (this includes members of the organization),
  the bot ignores the PR.
  
- If the user is not a collaborator, but has signed the CLA, the bot adds a `cla:yes` label.

- If the user is not a collaborator and has not signed the CLA, the bot adds a
  `cla:no` label and posts a comment reminding the user to sign the CLA. 
       
  The bot then listens to incoming comments in the PR. If the author of the
  comment is also the author of the PR ("pinging"), then the bot checks the
  CLA again. If the CLA is now signed, the bot posts a comment with a thank you message.
  Otherwise it ignores the comment.



[webhooks]: https://developer.github.com/webhooks/
[log4j]: https://docs.oracle.com/cd/E29578_01/webhelp/cas_webcrawler/src/cwcg_config_log4j_file.html
