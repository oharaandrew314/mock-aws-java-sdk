package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsMessage
import io.andrewohara.awsmock.sns.MockSnsV2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.NotFoundException
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import software.amazon.awssdk.services.sns.model.SnsException

class MockSnsV2PublishBatchTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    private val entry = PublishBatchRequestEntry.builder()
        .id("123")
        .message("hi")
        .messageAttributes(mapOf(
            "foo" to MessageAttributeValue.builder().dataType("String").stringValue("bar").build()
        ))
        .build()

    @Test
    fun `publishBatch to missing topic`() {
        shouldThrow<NotFoundException> {
            client.publishBatch {
                it.topicArn("arn:aws:sns:us-east-1:583125843759:foo")
                it.publishBatchRequestEntries(entry)
            }
        }
    }

    @Test
    fun `publishBatch without arn`() {
        backend.createTopic("foo")

        shouldThrow<SnsException> {
            client.publishBatch {
                it.publishBatchRequestEntries(entry)
            }
        }
    }

    @Test
    fun `publishBatch - success`() {
        val topic = backend.createTopic("foo")

        val result = client.publishBatch {
            it.topicArn(topic.arn)
            it.publishBatchRequestEntries(entry)
        }

        val resultEntry = result
            .successful()
            .shouldHaveSize(1)
            .first()

        topic.messages().shouldContainExactly(
            MockSnsMessage(
                subject = null,
                message = "hi",
                messageId = resultEntry.messageId(),
                attributes = mapOf("foo" to "bar")
            )
        )
    }
}