package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.*

class MockSqsV2ChangeMessageVisibilityBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)
    private val queue = backend.create("foo")!!

    @Test
    fun `change visibility for batch with a valid entry, invalid timeout, an invalid receipt handle, and a receipt handle that belongs to a different queue`() {
        queue.send("message1")
        queue.send("message2")

        val queue2 = backend.create("bar")!!
        queue2.send("message3")


        val (receipt1, receipt2) = queue.receive()
        val (receipt3) = queue2.receive()

        val result = client.changeMessageVisibilityBatch {
            it.queueUrl(queue.url)
            it.entries(
                ChangeMessageVisibilityBatchRequestEntry.builder().id("1").receiptHandle(receipt1.receiptHandle).visibilityTimeout(30).build(),
                ChangeMessageVisibilityBatchRequestEntry.builder().id("2").receiptHandle(receipt2.receiptHandle).visibilityTimeout(-1).build(),
                ChangeMessageVisibilityBatchRequestEntry.builder().id("3").receiptHandle(receipt3.receiptHandle).visibilityTimeout(30).build(),
                ChangeMessageVisibilityBatchRequestEntry.builder().id("4").receiptHandle("receiptHandle").visibilityTimeout(30).build()
            )
        }

        assertThat(result).isEqualTo(
            ChangeMessageVisibilityBatchResponse.builder()
                .successful(
                    ChangeMessageVisibilityBatchResultEntry.builder().id("1").build()
                )
                .failed(
                    BatchResultErrorEntry.builder().id("2").message("invalid visibility timeout: -1").build(),
                    BatchResultErrorEntry.builder().id("3").message("receipt handle invalid: ${receipt3.receiptHandle}").build(),
                    BatchResultErrorEntry.builder().id("4").message("receipt handle invalid: receiptHandle").build()
                )
                .build()
        )
    }

    @Test
    fun `change visibility for missing queue`() {
        queue.send("toggles")
        val (receipt) = queue.receive()

        assertThatThrownBy {
            client.changeMessageVisibilityBatch {
                it.queueUrl("missingQueue")
                it.entries(
                    ChangeMessageVisibilityBatchRequestEntry.builder().id("1").receiptHandle(receipt.receiptHandle).visibilityTimeout(30).build()
                )
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }
}