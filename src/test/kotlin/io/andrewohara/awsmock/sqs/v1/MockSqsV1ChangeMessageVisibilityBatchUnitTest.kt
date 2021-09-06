package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.SQSExceptions.toBatchResultErrorEntry
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsEmptyBatch
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import io.andrewohara.awsmock.sqs.SQSExceptions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class MockSqsV1ChangeMessageVisibilityBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)
    private val queue = backend.create("foo")!!

    @Test
    fun `change visibility for empty batch`() {
        val exception = catchThrowableOfType(
            { client.changeMessageVisibilityBatch(queue.url, listOf()) },
            AmazonSQSException::class.java
        )
        exception.assertIsEmptyBatch(ChangeMessageVisibilityBatchRequestEntry::class.java)
    }

    @Test
    fun `change visibility for batch with a valid entry, invalid timeout, an invalid receipt handle, and a receipt handle that belongs to a different queue`() {
        queue.send("message1")
        queue.send("message2")

        val queue2 = backend.create("bar")!!
        queue2.send("message3")


        val (receipt1, receipt2) = queue.receive()
        val (receipt3) = queue2.receive()

        val items = listOf(
            ChangeMessageVisibilityBatchRequestEntry("1", receipt1.receiptHandle).withVisibilityTimeout(30),
            ChangeMessageVisibilityBatchRequestEntry("2", receipt2.receiptHandle).withVisibilityTimeout(-1),
            ChangeMessageVisibilityBatchRequestEntry("3", receipt3.receiptHandle).withVisibilityTimeout(30),
            ChangeMessageVisibilityBatchRequestEntry("4", "missingHandle").withVisibilityTimeout(30),
        )
        val result = client.changeMessageVisibilityBatch(queue.url, items)

        assertThat(result).isEqualTo(
            ChangeMessageVisibilityBatchResult()
                .withSuccessful(ChangeMessageVisibilityBatchResultEntry().withId("1"))
                .withFailed(
                    SQSExceptions.createInvalidVisibilityTimeoutException(-1).toBatchResultErrorEntry("2"),
                    SQSExceptions.createInvalidReceiptHandleException().toBatchResultErrorEntry("3"),
                    SQSExceptions.createInvalidReceiptHandleException().toBatchResultErrorEntry("4"),
                )
        )
    }

    @Test
    fun `change visibility for missing queue`() {
        queue.send("toggles")
        val (receipt) = queue.receive()

        val exception = catchThrowableOfType(
            {
                client.changeMessageVisibilityBatch(
                    "missingQueue",
                    listOf(ChangeMessageVisibilityBatchRequestEntry("1", receipt.receiptHandle))
                )
            },
            AmazonSQSException::class.java
        )

        exception.assertIsQueueDoesNotExist()
    }
}