# Schema registry sync

[![Build Status](https://travis-ci.com/snowplow-product/schema-registry-sync.svg?token=rA744zFX5YFUNdyp1U6x&branch=master)](https://travis-ci.com/snowplow-product/schema-registry-sync)

CI process evaluating pull requests made to schema registries through [`igluctl`][iglu-ctl].
Results of the CI process are published as Github statuses and Zendesk tickets.

## Process flow

### Main algorithm

Schema registry sync (SRS for short) streams pull request events emitted by
[GitHub Webhooks][webhooks] from [AWS SQS][sqs].

Incoming pull request events are discarded if the pull request was not opened,
synchronized or reopened as the other events shouldn't trigger an SRS run (e.g.
labelling or closing).

If the pull request contains modifications or deletions of existing files, the
CI process is short-circuited and a failure status is uploaded to Github as
those changes are not allowed according to [the spec][spec].

If the pull request passes the previous check, we upload a pending status to
Github since we are now assured to run the CI process.

A list of the files modified by this pull request is retrieved and each of these
files are temporarily written out to disk in a specific folder.

The lint command of igluctl is run on this folder. Depending on the exit code
of the command, we either:

- upload a success status to Github if the command succeeded
- upload a failure status to Github and create a Zendesk ticket documenting
the failure (which is made of the combination of stderr and stdout) if the
command failed

We clean up the folder where we stored our schemas and repeat the process
indefinitely.

### Error handling

If Zendesk or Github were to give back an unexpected response (404 or the like),
the error will be logged in a local log file and the message which caused the
error will be discarded.

More graceful error handling is discussed in #19.

## Operations

### Webhook setup

To get Github events into SQS we need to setup a Github webhook.

To set up the webhook for a specific repository, we'll need to enable the SQS integration for this
repository (Settings > Integration and services > Add Service > Amazon SQS).

By default the SQS webhook doesn't monitor pull request events (only push events), that's something
we need to change by getting the id of the hook:

```bash
curl -H 'Authorization: token TOKEN' \
  https://api.github.com/repos/owner/repo/hooks
```

We'll get something like:

```json
[
  {
    "type": "Repository",
    "id": 13651457,
    "name": "sqsqueue",
    "active": true,
    "events": [
      "push"
    ],
    "config": {
      "aws_access_key": "ACCESS_KEY",
      "aws_secret_key": "SECRET_KEY",
      "aws_sqs_arn": "arn"
    },
    "updated_at": "2017-05-08T11:57:00Z",
    "created_at": "2017-05-08T11:57:00Z",
    "url": "https://api.github.com/repos/snowplow/snowplow/hooks/13651457",
    "test_url": "https://api.github.com/repos/snowplow/snowplow/hooks/13651457/test",
    "ping_url": "https://api.github.com/repos/snowplow/snowplow/hooks/13651457/pings",
    "last_response": {
      "code": 0,
      "status": "unused",
      "message": "Unused"
    }
  }
]
```

The hook id being 13651457, we can now edit it to monitor pull request events:

```bash
 curl -X PATCH -H "Authorization: token TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "active": true, "events": ["pull_request"] }' \
  'https://api.github.com/repos/snowplow/snowplow/hooks/13651457'
```

Pull request events will now be sent to SQS!

### AWS credentials

SRS uses the [DefaultAWSCredentialsProviderChain][dacpc] to get its AWS credentials.

The minimal IAM policy for the user whose credentials will be provided is:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmtid",
            "Effect": "Allow",
            "Action": [
                "sqs:DeleteMessage",
                "sqs:ReceiveMessage"
            ],
            "Resource": [
                "arn:aws:sqs:eu-central-1:{account-number}:{queue-name}"
            ]
        }
    ]
}
```

where `account-number` is the account number and `queue-name` is the name of
the queue.

### Igluctl setup

The [igluctl][iglu-ctl]'s command needs to be installed somewhere on the machine, refer to
its readme to download it.

### Configuration

Once all this setup is done, we can fill up our configuration file. An example configuration is
shown here:

```
qUrl = "https://sqs.eu-central-1.amazonaws.com/{account-number}/{queue-name}"
gh {
  token = aaaa
  context = snowplow-schema-registry-sync
}
zd {
  email = "email@snowplowanalytics.com"
  token = aaaa
  subdomain = snowplow
  assignee = 24580852
  tag = schema-registry-sync
}
workingDir = /tmp/schemas
igluctlLocation = /tmp/igluctl
```

Let's go through each in turn:

- `qUrl`: url of the SQS queue available in the AWS console
- `gh`:
  - `token`: a Github API token
  - `context`: text that will displayed as the generator of the status
- `zd`:
  - `email`: email of the agent that will be used to create ticket
  - `token`: a Zendesk API token linked to the agent whose email is used
  - `subdomain`: zendesk subdomain where tickets will be published
  - `assignee`: id of the group the ticket will be assigned to
  - `tag`: tag affected to the ticket
- `workingDir`: a directory where schemas will be temporarily downloaded to for validation
- `igluctlLocation`: path to the igluctl command

Note: to get the id of a Zendesk group:

```bash
curl https://subdomain.zendesk.com/api/v2/groups.json \
  -u email@snowplowanalytics.com/token:TOKEN
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
  -jar target/scala-2.11/schema-registry-sync-0.1.0.jar
```

### PROPRIETARY AND CONFIDENTIAL

Unauthorized copying of this project via any medium is strictly prohibited.

Copyright (c) 2017 Snowplow Analytics Ltd. All rights reserved.

[iglu-ctl]: https://github.com/snowplow/iglu/tree/master/0-common/igluctl
[webhooks]: https://developer.github.com/webhooks/
[sqs]: https://aws.amazon.com/sqs/
[log4j]: https://docs.oracle.com/cd/E29578_01/webhelp/cas_webcrawler/src/cwcg_config_log4j_file.html
[dacpc]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
[spec]: https://docs.google.com/document/d/1n-YqzQYo-APblAcHlxHMpU3MLlRxvCSeszET7XLSHzg/edit
