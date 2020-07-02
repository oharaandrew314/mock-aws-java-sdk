package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsInvalidReceiptHandle
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsInvalidReceiptHandleForQueue
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsInvalidVisibilityTimeout
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ChangeMessageVisibilityUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `change visibility for invalid receipt handle`() {
        val queue = client.createQueue("lolcats")

        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.queueUrl, "invalidHandle", 30) },
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
    fun `change visibility for message that belongs to different queue`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "Smokey")
        val received = client.receiveMessage(queue.queueUrl).messages.first()
        val otherQueue = client.createQueue("trolldogs")

        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(otherQueue.queueUrl, received.receiptHandle, 30) },
                AmazonSQSException::class.java
        )

        exception.assertIsInvalidReceiptHandleForQueue(received.receiptHandle)
    }

    @Test
    fun `change visibility to negative amount`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "Toggles")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.queueUrl, received.receiptHandle, -10) },
                AmazonSQSException::class.java
        )
        exception.assertIsInvalidVisibilityTimeout(-10)
    }

    @Test
    fun `change visibility to max int`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "Toggles")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val exception = catchThrowableOfType(
                { client.changeMessageVisibility(queue.queueUrl, received.receiptHandle, Int.MAX_VALUE) },
                AmazonSQSException::class.java
        )
        exception.assertIsInvalidVisibilityTimeout(Int.MAX_VALUE)
    }

    @Test
    fun `change visibility to 30 seconds`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "Toggles")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val result = client.changeMessageVisibility(queue.queueUrl, received.receiptHandle, 30)
        assertThat(result).isNotNull
    }
}