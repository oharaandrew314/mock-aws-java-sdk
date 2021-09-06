package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.backend.MockSqsMessage
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class MockSqsV2SendMessageUnitTest {

    private val time = Instant.ofEpochSecond(9001)
    private val backend = MockSqsBackend(Clock.fixed(time, ZoneId.of("UTC")))
    private val client = MockSqsV2(backend)
    private val queue = backend.create("lolcats")!!

    @Test
    fun `send message to missing queue`() {
        assertThatThrownBy {
            client.sendMessage {
                it.queueUrl("missingQueue")
                it.messageBody("foo")
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `send message`() {
        val result = client.sendMessage {
            it.queueUrl(queue.url)
            it.messageBody("meow")
        }

        assertThat(result.messageId()).isNotNull.isNotEmpty
        assertThat(queue.messages).containsExactly(
            MockSqsMessage(
                id = result.messageId(),
                body = "meow",
                visibleAt = time
            )
        )
    }

    @Test
    fun `send message with delay`() {
        val result = client.sendMessage {
            it.queueUrl(queue.url)
            it.messageBody("meow")
            it.delaySeconds(30)
        }

        assertThat(result.messageId()).isNotNull.isNotEmpty
        assertThat(queue.messages).containsExactly(
            MockSqsMessage(
                id = result.messageId(),
                body = "meow",
                visibleAt = time.plusSeconds(30)
            )
        )
    }
}