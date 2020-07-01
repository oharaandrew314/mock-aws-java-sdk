# mock-aws-sdk-java

A library that lets you mock AWS out of your tests, allowing you to achieve for better coverage with far less hassle.

## Install 

TODO()

## Quickstart

Any well-designed class with external dependencies will let you inject them, so the same follows for any AWS mediator.
Just modify your class to accept an instance of the AWS interface you need.

```kotlin
class ImportantFileProcessor (s3: AmazonS3? = null) {
    private val s3Client = s3 ?: AmazonS3ClientBuilder.defaultClient()

    fun process() {
        // do stuff with the s3Client...
    }
}

class ImportantFileProcessorUnitTest {
    
    private lateinit var s3Client: AmazonS3
    private lateinit var testObj: ImportantFileProcessor

    @Before
    fun setup() {
        s3Client = MockAmazonS3()
        testObj = ImportantFileProcessor(s3Client)
    }

    @Test
    fun `process default dataset`() {
        // use s3Client to initialize state

        testObj.process()

        // use s3Client to verify state
        // verify rest of state
    }   
}
```

## Samples

There are a variety of sample projects available to help get you started.

TODO link to samples

## Supported Services

| Service | Support | Mock Class |
| ------- | ------- | ---------- |
| S3 | Core Methods Done | io.andrewohara.awsmock.s3.MockAmazonS3() |