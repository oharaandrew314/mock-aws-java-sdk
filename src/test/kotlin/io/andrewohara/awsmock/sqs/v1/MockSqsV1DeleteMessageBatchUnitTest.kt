package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1DeleteMessageBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)
    private val queue = backend.create("lolcats")!!.also {
        it.send("meow")
    }
    private val receipt = queue.receive().first()

    @Test
    fun `delete batch with invalid receipt handles`() {
        val result = client.deleteMessageBatch(queue.url, listOf(
                DeleteMessageBatchRequestEntry("message1", "invalid")
        ))

        assertThat(result.successful).isEmpty()
        assertThat(result.failed).containsExactly(
                BatchResultErrorEntry()
                        .withId("message1")
                        .withCode("ReceiptHandleIsInvalid")
                        .withMessage("The input receipt handle is invalid")
                        .withSenderFault(true)
        )
    }

    @Test
    fun `delete batch with valid receipt handle for missing queue`() {
        val exception = catchThrowableOfType(
            { client.deleteMessageBatch("missingQueue", listOf(DeleteMessageBatchRequestEntry("message1", receipt.receiptHandle))) },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `delete batch`() {
        queue.send("hiss")
        val receipt2 = queue.receive().first()

        val result = client.deleteMessageBatch(queue.url, listOf(
            DeleteMessageBatchRequestEntry("message1", receipt.receiptHandle),
            DeleteMessageBatchRequestEntry("message2", receipt2.receiptHandle)
        ))

        assertThat(result).isEqualTo(
            DeleteMessageBatchResult()
                .withSuccessful(
                    DeleteMessageBatchResultEntry().withId("message1"),
                    DeleteMessageBatchResultEntry().withId("message2"),
                )
        )
    }

    @Test
    fun `delete batch with already deleted messages`() {
        queue.delete(receipt.receiptHandle)

        val result = client.deleteMessageBatch(queue.url, listOf(DeleteMessageBatchRequestEntry("message1", receipt.receiptHandle)))

        assertThat(result).isEqualTo(
            DeleteMessageBatchResult()
                .withSuccessful(DeleteMessageBatchResultEntry().withId("message1"))
        )
    }
}