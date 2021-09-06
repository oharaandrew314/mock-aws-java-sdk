package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsInvalidReceiptHandle
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsInvalidVisibilityTimeout
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1ChangeMessageVisibilityUnitTest {

    private val backend = MockSqsBackend()
    private val queue = backend.create("foo")!!.also {
        it.send("foo")
    }
    private val client = MockSqsV1(backend)

    private val receipt = queue.receive().first()

    @Test
    fun `change visibility for invalid receipt handle`() {
        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.url, "invalidHandle", 30) },
                AmazonSQSException::class.java
        )
        exception.assertIsInvalidReceiptHandle()
    }

    @Test
    fun `change visibility for missing queue`() {
        val exception = catchThrowableOfType(
                { client.changeMessageVisibility("missingQueue", "invalidHandle", 30) },
                AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `change visibility to negative amount`() {
        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.url, receipt.receiptHandle, -10) },
                AmazonSQSException::class.java
        )
        exception.assertIsInvalidVisibilityTimeout(-10)
    }

    @Test
    fun `change visibility to max int`() {
        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.url, receipt.receiptHandle, Int.MAX_VALUE) },
                AmazonSQSException::class.java
        )
        exception.assertIsInvalidVisibilityTimeout(Int.MAX_VALUE)
    }

    @Test
    fun `change visibility to 30 seconds`() {
        val result = client.changeMessageVisibility(queue.url, receipt.receiptHandle, 30)
        assertThat(result).isNotNull
    }
}