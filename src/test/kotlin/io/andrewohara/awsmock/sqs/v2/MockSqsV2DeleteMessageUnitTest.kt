package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException

class MockSqsV2DeleteMessageUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)
    private val queue = backend.create("lolcats")!!

    @Test
    fun `delete message with missing receipt handle`() {
        assertThatThrownBy {
            client.deleteMessage {
                it.queueUrl(queue.url)
                it.receiptHandle("receiptHandle")
            }
        }.isInstanceOf(ReceiptHandleIsInvalidException::class.java)
    }

    @Test
    fun `delete message`() {
        queue.send("meow")
        val (receipt) = queue.receive()

        val result = client.deleteMessage {
            it.queueUrl(queue.url)
            it.receiptHandle(receipt.receiptHandle)
        }

        assertThat(result).isEqualTo(DeleteMessageResponse.builder().build())
        assertThat(queue.messages).isEmpty()
    }

    @Test
    fun `delete message that's already been deleted`() {
        queue.send("meow")
        val (receipt) = queue.receive()
        queue.delete(receipt.receiptHandle)

        // delete second time with no error
        client.deleteMessage {
            it.queueUrl(queue.url)
            it.receiptHandle(receipt.receiptHandle)
        }
    }
}