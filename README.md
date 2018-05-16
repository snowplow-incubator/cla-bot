# Schema registry sync

[![Build Status](https://travis-ci.com/snowplow-incubator/cla-bot.svg?token=rA744zFX5YFUNdyp1U6x&branch=master)](https://travis-ci.com/snowplow-incubator/cla-bot)

## Process flow

### Main algorithm


### Error handling

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

### Configuration

Once all this setup is done, we can fill up our configuration file. An example configuration is
shown here:

```
```

Let's go through each in turn:

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

[webhooks]: https://developer.github.com/webhooks/
[sqs]: https://aws.amazon.com/sqs/
[log4j]: https://docs.oracle.com/cd/E29578_01/webhelp/cas_webcrawler/src/cwcg_config_log4j_file.html
[dacpc]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html
