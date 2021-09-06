package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse

class MockSqsV2ReceiveMessageUnitTest {

    private val backend = MockSqsBackend()
    private val testObj = MockSqsV2(backend)
    private val queue = backend.create("lolcats")!!

    @Test
    fun `receive message from missing queue`() {
        assertThatThrownBy {
            testObj.receiveMessage {
                it.queueUrl("missingUrl")
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `receive message from empty queue`() {
        val result = testObj.receiveMessage {
            it.queueUrl(queue.url)
        }

        assertThat(result).isEqualTo(
            ReceiveMessageResponse.builder()
                .messages(emptyList())
                .build()
        )
    }

    @Test
    fun `receive single message from queue`() {
        val message = queue.send("meow")

        val result = testObj.receiveMessage {
            it.queueUrl(queue.url)
        }
        assertThat(result.messages()).hasSize(1)

        val received = result.messages().first()
        assertThat(received.messageId()).isNotNull.isEqualTo(message.id)
        assertThat(received.body()).isEqualTo("meow")
        assertThat(received.receiptHandle()).isNotEmpty
    }

    @Test
    fun `receive message while only message has already been received`() {
        queue.send("meow")
        queue.receive()

        val result = testObj.receiveMessage {
            it.queueUrl(queue.url)
        }

        assertThat(result.messages()).isEmpty()
    }

    @Test
    fun `receive second message after first message has been received`() {
        queue.send("meow")
        val message2 = queue.send("hiss")

        queue.receive(limit = 1)

        val result = testObj.receiveMessage {
            it.queueUrl(queue.url)
        }

        assertThat(result.messages()).hasSize(1)
        val receipt = result.messages().first()
        assertThat(receipt.messageId()).isEqualTo(message2.id)
    }
}