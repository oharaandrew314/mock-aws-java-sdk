package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsEmptyBatch
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class SendMessageBatchUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `send empty batch`() {
        val queue = client.createQueue("foo")

        val exception = catchThrowableOfType(
                { client.sendMessageBatch(queue.queueUrl, listOf()) },
                AmazonSQSException::class.java
        )
        exception.assertIsEmptyBatch(SendMessageBatchRequestEntry::class.java)
    }

    @Test
    fun `send batch to missing queue`() {
        val entries = listOf(
                SendMessageBatchRequestEntry().withMessageBody("foo")
        )

        val exception = catchThrowableOfType({ client.sendMessageBatch("missingUrl", entries) }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `send batch`() {
        val queue = client.createQueue("foo")
        val entries = listOf(
                SendMessageBatchRequestEntry().withMessageBody("bar")
        )

        val result = client.sendMessageBatch(queue.queueUrl, entries)

        assertThat(result.failed).isEmpty()
        assertThat(result.successful).hasSize(1)
    }
}