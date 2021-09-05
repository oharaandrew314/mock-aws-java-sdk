![Test](https://github.com/oharaandrew314/mock-aws-java-sdk/workflows/Test/badge.svg)
[![codecov](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)

# mock-aws-java-sdk

A test library providing mocked versions of AWS SDK clients, for integration with your tests.

## Requirements

- java 8 and above
- AWS SDKs are not bundled, so you can pick and choose which ones you want

## Install

[![](https://jitpack.io/v/oharaandrew314/mock-aws-java-sdk.svg)](https://jitpack.io/#oharaandrew314/mock-aws-java-sdk)

Follow the instructions on Jitpack.

## QuickStart

Just make a mocked client and inject it into your classes that uses AWS.  Perform any initialization you need, and test away!

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

| Service | SDKs | Support |
| ------- | ---- | ------- | 
| S3 | v1, v2 | :heavy_check_mark: Core Functionality |
| SQS | v1 | :heavy_check_mark: Core Functionality |
| Dynamo DB | v1 | :heavy_check_mark: Core Functionality<br/>:heavy_check_mark: Mapper<br/>:x: query expressions<br/>:x: conditional operations<br/> |
| SSM | v1 | :heavy_check_mark: Parameter Store |
| Secrets Manager | v1 | :heavy_check_mark: Core Functionality<br/>:x: Secret Rotation |
| SNS | v1, v2 | :heavy_check_mark: Create/Delete Topic<br/>:heavy_check_mark: Publish to Topic |
| Cloudformation | v1, v2 | :heavy_check_mark: Exports Only |

## Samples

There are some [Sample Snippets](https://github.com/oharaandrew314/mock-aws-java-sdk/tree/master/src/test/kotlin/io/andrewohara/awsmock/samples) available to help get you started.

## How it Works

Each mocked SDK implements the same interface as the standard SDKs.
While the standard ones will delegate the calls to the AWS REST API,
the mocked one will perform the operations in-memory instead.

Instead of allowing your classes under test to initialize their own SDK,
you should update them to allow the SDK to be injected;
your main runner will inject real SDKs, and your tests will inject the mocks.

Since the AWS resources created in the mocks are in-memory, they will not persist,
and they will not be shared between clients.
However, all the mocks with support for the v2 sdk will allow you to inject the backend into the client,
allowing it to be shared between clients.

## Gotchas

- most services will not yet respect the passage of time