package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.*
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsMessage
import io.andrewohara.awsmock.sns.MockSnsV1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class MockSnsV1PublishBatchTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    private val entry = PublishBatchRequestEntry()
        .withId("123")
        .withMessage("hi")
        .withMessageAttributes(mapOf(
            "foo" to MessageAttributeValue().withDataType("String").withStringValue("bar")
        ))

    @Test
    fun `publishBatch to missing topic`() {
        shouldThrow<NotFoundException> {
            client.publishBatch(
                PublishBatchRequest()
                    .withTopicArn("arn:aws:sns:us-east-1:583125843759:foo")
                    .withPublishBatchRequestEntries(entry)
            )
        }
    }

    @Test
    fun `publishBatch without arn`() {
        backend.createTopic("foo")

        shouldThrow<AmazonSNSException> {
            client.publishBatch(
                PublishBatchRequest()
                    .withPublishBatchRequestEntries(entry)
            )
        }
    }

    @Test
    fun `publishBatch - success`() {
        val topic = backend.createTopic("foo")

        val result = client.publishBatch(
            PublishBatchRequest()
                .withTopicArn(topic.arn)
                .withPublishBatchRequestEntries(entry)
        )

        val resultEntry = result
            .successful
            .shouldHaveSize(1)
            .first()

        topic.messages().shouldContainExactly(
            MockSnsMessage(
                subject = null,
                message = "hi",
                messageId = resultEntry.messageId,
                attributes = mapOf("foo" to "bar")
            )
        )
    }

    @Test
    fun `publishBatch - over batch size`() {
        val topic = backend.createTopic("foo")

        shouldThrow<AmazonSNSException> {
            val entries = (1..11).map { entry }
            client.publishBatch(
                PublishBatchRequest()
                    .withTopicArn(topic.arn)
                    .withPublishBatchRequestEntries(entries)
            )
        }
    }
}