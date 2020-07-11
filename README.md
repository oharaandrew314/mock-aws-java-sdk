![Test](https://github.com/oharaandrew314/mock-aws-java-sdk/workflows/Test/badge.svg)
[![codecov](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)

# mock-aws-java-sdk

A library that lets you mock AWS out of your tests, allowing you to achieve for better coverage with far less hassle.


## Requirements

- java 8 and above
- aws-java-sdk-\<service\> of your choice as they are not provided by this package; versions `1.11.300` and above

## Install 

Install the latest all-in-one package from Jitpack.

[![](https://jitpack.io/v/oharaandrew314/mock-aws-java-sdk.svg)](https://jitpack.io/#oharaandrew314/mock-aws-java-sdk)

## QuickStart

Any well-designed class will let you inject its dependencies, so the same can apply to your AWS mediators.
Just inject the provided mocks into your business logic, then unit-test them as if they were connected to AWS. 

```java
public class QuickStart {

    private final AmazonS3 s3Client;
    private final String bucket;

    public QuickStart(String bucket, AmazonS3 s3Client) {
        this.bucket = bucket;
        this.s3Client = s3Client;
    }

    public List<String> process() {
        return s3Client.listObjectsV2(bucket)
                .getObjectSummaries()
                .stream()
                .map(summary -> s3Client.getObjectAsString(bucket, summary.getKey()))
                .collect(Collectors.toList());
    }
}

```

```java
public class QuickStartUnitTest {

    private final AmazonS3 s3Client = new MockAmazonS3();
    private final QuickStart testObj = new QuickStart("bucket", s3Client);

    @Test
    public void processTwoFiles() {
        // initialize state
        s3Client.createBucket("bucket");
        s3Client.putObject("bucket", "file1.txt", "special content");
        s3Client.putObject("bucket", "file2.txt", "secret content");

        // perform test
        final List<String> result = testObj.process();

        // verify result
        Assertions.assertThat(result).containsExactlyInAnyOrder("special content", "secret content");
    }
}
```

## Supported Services

| Service | Support | Interface | Mock Implementation |
| ------- | ------- | --------- | ------------------- |
| S3 | Core Functionality | [AmazonS3](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3.html) | io.andrewohara.awsmock.s3.MockAmazonS3() |
| SQS | Core Functionality | [AmazonSQS](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sqs/AmazonSQS.html) | io.andrewohara.awsmock.sqs.MockAmazonSQS() |
| Dynamo DB | Core Functionality | [AmazonDynamoDB](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDB.html) | io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB |

## Samples

There are some [Sample Snippets](https://github.com/oharaandrew314/mock-aws-java-sdk/tree/master/src/test/kotlin/io/andrewohara/awsmock/samples) available to help get you started.

## How it Works

This tool isn't meant to be an AWS emulator, so it won't persist your data, it might abstract a few intricacies away, and it might not always match exactly what AWS would give you.

Instead, this is meant to be a unit and integration testing aid to give you a close approximation of how your code will run in a real environment.
To that end, this tool provides you with select implementations of AWS client interfaces.
When you inject these mocks into your business logic during tests, the commands will bypass AWS and be handled by custom, in-memory implementations.

Since only the low-level interfaces are overridden, the Dynamo DB Document and Mapper interfaces will work as long as they are initialized with a mock. 

## Gotchas

- content-type cannot be inferred in s3 file uploads on osx-java8 due to a [jvm bug](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7133484)
- Dynamo DB query expressions are not supported (conditions will work)

## Want to Help?

You can:

- submit issues for errors that don't match what AWS would return
- Submit a PR to increase the level of implementation for the currently supported services
- submit a PR for a new supported service
