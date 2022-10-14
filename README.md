![Test](https://github.com/oharaandrew314/mock-aws-java-sdk/workflows/Test/badge.svg)
[![codecov](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/oharaandrew314/mock-aws-java-sdk)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-blue.svg)](http://unlicense.org/)

# mock-aws-java-sdk

Provides mocked versions of the Java AWS SDK clients, for use in unit tests.
Supporting:
- V1 Java SDK
- V2 Java SDK

## Requirements

- java 8 and above
- AWS SDKs are not bundled, so you can pick and choose which ones you want

## Install

[![](https://jitpack.io/v/oharaandrew314/mock-aws-java-sdk.svg)](https://jitpack.io/#oharaandrew314/mock-aws-java-sdk)

Follow the instructions on Jitpack.

## QuickStart

```kotlin
class GameService(private val sns: SnsClient, private val eventsTopicArn: String) {

    private val gamesDao = mutableMapOf<UUID, String>()

    operator fun get(id: UUID) = gamesDao[id]

    fun createGame(name: String): UUID {
        val id = UUID.randomUUID()
        gamesDao[id] = name

        sns.publish {
            it.topicArn(eventsTopicArn)
            it.message(name)
        }

        return id
    }
}

// inject a real client for real use
fun main(args: Array<String>) {
    val eventsTopicArn = args.first()
    val sns = SnsClient.create()
    val service = GameService(sns, eventsTopicArn)
    // configure API and start server...
}

class GameServiceTest {

    private val backend = MockSnsBackend()
    private val topic = backend.createTopic("game-events")
    
    // inject a mock client for tests
    private val testObj = GameService(
        sns = MockSnsV2(backend),
        eventsTopicArn = topic.arn
    )

    @Test
    fun `create game`() {
        val id = testObj.createGame("Mass Effect 3")
        Assertions.assertThat(testObj[id]).isEqualTo("Mass Effect 3")
        Assertions.assertThat(topic.messages().map { it.message }).containsExactly("Mass Effect 3")
    }
}
```

## How it Works

Each mocked SDK implements the same interface as the standard SDKs.
While the standard ones will delegate the calls to the AWS REST API,
these clients will delegate them to a mock backend.

Instead of allowing your classes under test to initialize their own SDK,
you should update them to allow the SDK to be injected;
your main runner will inject real SDKs, and your tests will inject the mocks.

**Note:** Since the AWS resources created in the mock backends are in-memory, they will not persist.

**Note:** If you use the v1 SDK, make sure you inject the v1 mock.  The same applies for the v2 SDK. 


## Supported Services

| Service | SDKs | Support |
| ------- | ---- | ------- | 
| S3 | [MockS3V1](src/main/kotlin/io/andrewohara/awsmock/s3/MockS3V1.kt)<br/>[MockS3V2](src/main/kotlin/io/andrewohara/awsmock/s3/MockS3V2.kt) | :heavy_check_mark: Core Functionality<br/>:x: Object Metadata<br/>:x: Permissions |
| SQS | [MockSqsV1](src/main/kotlin/io/andrewohara/awsmock/sqs/MockSqsV1.kt)<br/>[MockSqsV2](src/main/kotlin/io/andrewohara/awsmock/sqs/MockSqsV2.kt) | :heavy_check_mark: Core Functionality<br/>:x: Message Attributes |
| Dynamo DB | [MockDynamoDbV1](src/main/kotlin/io/andrewohara/awsmock/dynamodb/MockDynamoDbV1.kt)<br/>[MockDynamoDbV2](src/main/kotlin/io/andrewohara/awsmock/dynamodb/MockDynamoDbV2.kt) | :heavy_check_mark: Core Functionality<br/>:heavy_check_mark: Mapper<br/>:heavy_check_mark: Conditions<br/>:eight_pointed_black_star: (Partial) Filter Expressions<br/>:x: Conditional Operations<br/>:x: Projections |
| SSM | [MockSsmV1](src/main/kotlin/io/andrewohara/awsmock/ssm/MockSsmV1.kt)<br/>[MockSsmV2](src/main/kotlin/io/andrewohara/awsmock/ssm/MockSsmV2.kt) | :heavy_check_mark: Parameter Store |
| Secrets Manager | [MockSecretsManagerV1](src/main/kotlin/io/andrewohara/awsmock/secretsmanager/MockSecretsManagerV1.kt)<br/>[MockSecretsManagerV2](src/main/kotlin/io/andrewohara/awsmock/secretsmanager/MockSecretsManagerV2.kt) | :heavy_check_mark: Core Functionality<br/>:x: Secret Rotation |
| SNS | [MockSnsV1](src/main/kotlin/io/andrewohara/awsmock/sns/MockSnsV1.kt)<br/>[MockSnsV2](src/main/kotlin/io/andrewohara/awsmock/sns/MockSnsV2.kt) | :heavy_check_mark: Create/Delete Topic<br/>:heavy_check_mark: Publish to Topic<br/>:x: Subscriptions |
| Cloudformation | [MockCloudformationV1](src/main/kotlin/io/andrewohara/awsmock/cloudformation/MockCloudformationV1.kt)<br/>[MockCloudformationV2](src/main/kotlin/io/andrewohara/awsmock/cloudformation/MockCloudformationV2.kt) | :heavy_check_mark: Exports Only |

## Samples

There are some [Sample Snippets](src/test/kotlin/io/andrewohara/awsmock/samples) available to help get you started.
