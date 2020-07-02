package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.SQSExceptions.toBatchResultErrorEntry
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsEmptyBatch
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions
import org.junit.Test

class ChangeMessageVisibilityBatchUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `change visibility for empty batch`() {
        val queue = client.createQueue("foo")

        val exception = Assertions.catchThrowableOfType(
                { client.changeMessageVisibilityBatch(queue.queueUrl, listOf()) },
                AmazonSQSException::class.java
        )
        exception.assertIsEmptyBatch(ChangeMessageVisibilityBatchRequestEntry::class.java)
    }

    @Test
    fun `change visibility for batch with a valid entry, invalid timeout, an invalid receipt handle, and wrong queue`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "message1")
        client.sendMessage(queue.queueUrl, "message2")
        val (message1, message2) = client.receiveMessage(ReceiveMessageRequest(queue.queueUrl).withMaxNumberOfMessages(2)).messages

        val otherQueue = client.createQueue("trolldogs")
        client.sendMessage(otherQueue.queueUrl, "message4")
        val message4 = client.receiveMessage(otherQueue.queueUrl).messages.first()

        val items = listOf(
                ChangeMessageVisibilityBatchRequestEntry("1", message1.receiptHandle).withVisibilityTimeout(30),
                ChangeMessageVisibilityBatchRequestEntry("2", message2.receiptHandle).withVisibilityTimeout(-1),
                ChangeMessageVisibilityBatchRequestEntry("3", "fooHandle").withVisibilityTimeout(30),
                ChangeMessageVisibilityBatchRequestEntry("4", message4.receiptHandle).withVisibilityTimeout(30)
        )
        val result = client.changeMessageVisibilityBatch(queue.queueUrl, items)

        Assertions.assertThat(result).isEqualTo(
                ChangeMessageVisibilityBatchResult()
                        .withSuccessful(ChangeMessageVisibilityBatchResultEntry().withId("1"))
                        .withFailed(
                                SQSExceptions.createInvalidParameterException().toBatchResultErrorEntry("2"),
                                SQSExceptions.createInvalidReceiptHandleException().toBatchResultErrorEntry("3"),
                                SQSExceptions.createInvalidReceiptHandleForQueueException(message4.receiptHandle).toBatchResultErrorEntry("4")
                        )
        )
    }

    @Test
    fun `change visibility for missing queue`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "Toggles")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val exception = Assertions.catchThrowableOfType(
                { client.changeMessageVisibilityBatch(
                        "missingQueue",
                        listOf(ChangeMessageVisibilityBatchRequestEntry("1", received.receiptHandle))
                ) },
                AmazonSQSException::class.java
        )

        exception.assertIsQueueDoesNotExist()
    }
}