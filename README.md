![Test](https://github.com/oharaandrew314/mock-aws-java-sdk/workflows/Test/badge.svg)
[![codecov](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)

# mock-aws-java-sdk

A library that lets you mock AWS out of your tests, allowing you to achieve for better coverage with far less hassle.


## Requirements

- java 8 and above
- aws-java-sdk-\<service\> of your choice as they are not provided by this package; versions `1.11.300` and above

## Gotchas

- content-type cannot be inferred in file uploads on osx-java8 due to a [jvm bug](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=7133484)


## Install 

Install the latest all-in-one package from Jitpack.

[![](https://jitpack.io/v/oharaandrew314/mock-aws-java-sdk.svg)](https://jitpack.io/#oharaandrew314/mock-aws-java-sdk)

## QuickStart

Any well-designed class will let you inject its dependencies, so the same can apply to your AWS mediators.
Just modify them to accept an implementation of the AWS client interface, and then inject the mocked version during your tests.

```java
// QuickStart.java

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
// QuickStartUnitTest.java

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
        Assertions.assertThat(result).containsExactly("special content", "secret content");
    }
}
```

## Supported Services

| Service | Support | Mock Class |
| ------- | ------- | ---------- |
| S3 | Core Functionality Done | io.andrewohara.awsmock.s3.MockAmazonS3() |
| SQS | Core Functionality Done | io.andrewohara.awsmock.sqs.MockAmazonSQS() |

## Samples

There are a variety of sample projects available to help get you started.

[Check out the samples](https://github.com/oharaandrew314/mock-aws-java-sdk/tree/master/src/test/kotlin/io/andrewohara/awsmock/samples)