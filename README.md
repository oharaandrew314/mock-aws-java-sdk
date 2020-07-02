# mock-aws-sdk-java

A library that lets you mock AWS out of your tests, allowing you to achieve for better coverage with far less hassle.

## Install 

TODO()

## QuickStart

Any well-designed class will let you inject its dependencies, so the same can apply to your AWS mediators.
Just modify them to accept an implementation of the AWS client interface, and then inject the mocked version during your tests.

```java
// ImportantFileProcessor.java

public class ImportantFileProcessor {

    private final AmazonS3 s3Client;

    public ImportantFileProcessor() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    public ImportantFileProcessor(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void process() {
        // do stuff with the s3Client
    }
}
```

```java
// ImportantFileProcessorUnitTest.java

public class ImportantFileProcessorUnitTest {

    private final AmazonS3 s3Client = new MockAmazonS3();
    private final ImportFileProcessor testObj = new ImportantFileProcessor(s3Client);

    @Test
    public void processDefaultDataset() {
        // use s3Client to initialize state

        testObj.process();

        // use s3Client to verify state
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

[Check out the samples](https://github.com/oharaandrew314/mock-aws-sdk-java/tree/master/src/test/kotlin/io/andrewohara/awsmock/samples)