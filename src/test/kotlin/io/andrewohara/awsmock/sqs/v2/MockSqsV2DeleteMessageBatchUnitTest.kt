package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.*

class MockSqsV2DeleteMessageBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)
    private val queue = backend.create("lolcats")!!.also {
        it.send("meow")
    }
    private val receipt = queue.receive().first()

    @Test
    fun `delete batch with invalid receipt handles`() {
        val result = client.deleteMessageBatch {
            it.queueUrl(queue.url)
            it.entries(
                DeleteMessageBatchRequestEntry.builder().id("message1").receiptHandle("invalid").build()
            )
        }

        assertThat(result).isEqualTo(
            DeleteMessageBatchResponse.builder()
                .successful(emptyList())
                .failed(
                    BatchResultErrorEntry.builder().id("message1").message("receiptHandle not found: invalid").build()
                )
                .build()
        )
    }

    @Test
    fun `delete batch with valid receipt handle for missing queue`() {
        assertThatThrownBy {
            client.deleteMessageBatch {
                it.queueUrl("missingQueue")
                it.entries(
                    DeleteMessageBatchRequestEntry.builder().id("message1").receiptHandle(receipt.receiptHandle).build()
                )
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `delete batch`() {
        queue.send("hiss")
        val receipt2 = queue.receive().first()

        val result = client.deleteMessageBatch {
            it.queueUrl(queue.url)
            it.entries(
                DeleteMessageBatchRequestEntry.builder().id("message1").receiptHandle(receipt.receiptHandle).build(),
                DeleteMessageBatchRequestEntry.builder().id("message2").receiptHandle(receipt2.receiptHandle).build()
            )
        }

        assertThat(result).isEqualTo(
            DeleteMessageBatchResponse.builder()
                .successful(
                    DeleteMessageBatchResultEntry.builder().id("message1").build(),
                    DeleteMessageBatchResultEntry.builder().id("message2").build(),
                )
                .failed(emptyList())
                .build()
        )
    }

    @Test
    fun `delete batch with already deleted messages`() {
        queue.delete(receipt.receiptHandle)

        val result = client.deleteMessageBatch {
            it.queueUrl(queue.url)
            it.entries(
                DeleteMessageBatchRequestEntry.builder().id("message1").receiptHandle(receipt.receiptHandle).build(),
            )
        }

        assertThat(result).isEqualTo(
            DeleteMessageBatchResponse.builder()
                .successful(
                    DeleteMessageBatchResultEntry.builder().id("message1").build(),
                )
                .failed(emptyList())
                .build()
        )
    }
}