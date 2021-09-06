package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.SendMessageRequest
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.backend.MockSqsMessage
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockSqsV1SendMessageUnitTest {

    private val time = Instant.ofEpochSecond(9001)
    private val backend = MockSqsBackend(Clock.fixed(time, ZoneId.of("UTC")))
    private val client = MockSqsV1(backend)
    private val queue = backend.create("lolcats")!!

    @Test
    fun `send message to missing queue`() {
        val exception = catchThrowableOfType(
            { client.sendMessage("missingQueue", "foo") },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `send message`() {
        val result = client.sendMessage(queue.url, "meow")

        assertThat(result.messageId).isNotNull.isNotEmpty
        assertThat(queue.messages).containsExactly(
            MockSqsMessage(
                id = result.messageId,
                body = "meow",
                visibleAt = time
            )
        )
    }

    @Test
    fun `send message with delay`() {
        val request = SendMessageRequest()
            .withMessageBody("meow")
            .withQueueUrl(queue.url)
            .withDelaySeconds(30)
        val result = client.sendMessage(request)

        assertThat(result.messageId).isNotNull.isNotEmpty
        assertThat(queue.messages).containsExactly(
            MockSqsMessage(
                id = result.messageId,
                body = "meow",
                visibleAt = time.plusSeconds(30)
            )
        )
    }
}