package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import io.andrewohara.awsmock.sqs.SQSExceptions.toBatchResultErrorEntry
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DeleteMessageBatchUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `delete batch with invalid receipt handles`() {
        val queue = client.createQueue("lolcats")

        val result = client.deleteMessageBatch(queue.queueUrl, listOf(
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
    fun `delete batch with valid receipt handle but wrong (yet existing) queue`() {
        val wrongQueue = client.createQueue("trolldogs")
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val result = client.deleteMessageBatch(wrongQueue.queueUrl, listOf(
                DeleteMessageBatchRequestEntry("message1", received.receiptHandle)
        ))

        assertThat(result.successful).isEmpty()
        assertThat(result.failed).containsExactly(
                SQSExceptions.createInvalidReceiptHandleForQueueException(received.receiptHandle).toBatchResultErrorEntry("message1")
        )
    }

    @Test
    fun `delete batch with valid receipt handle for missing queue`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val exception = catchThrowableOfType(
                { client.deleteMessageBatch("missingQueue", listOf(DeleteMessageBatchRequestEntry("message1", received.receiptHandle))) },
                AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `delete batch`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        client.sendMessage(queue.queueUrl, "hiss")
        val received = client.receiveMessage(ReceiveMessageRequest().withQueueUrl(queue.queueUrl).withMaxNumberOfMessages(10)).messages

        val result = client.deleteMessageBatch(queue.queueUrl, received.map { DeleteMessageBatchRequestEntry("message1", it.receiptHandle) })

        assertThat(result.successful).isEqualTo(received.map { DeleteMessageBatchResultEntry().withId("message1") })
        assertThat(result.failed).isEmpty()
    }

    @Test
    fun `delete batch with already deleted messages`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()
        client.deleteMessage(queue.queueUrl, received.receiptHandle)

        val result = client.deleteMessageBatch(queue.queueUrl, listOf(DeleteMessageBatchRequestEntry("message1", received.receiptHandle)))

        assertThat(result.successful).containsExactly(DeleteMessageBatchResultEntry().withId("message1"))
        assertThat(result.failed).isEmpty()
    }
}