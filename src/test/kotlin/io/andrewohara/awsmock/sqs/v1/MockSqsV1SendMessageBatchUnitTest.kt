package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry
import io.andrewohara.awsmock.sqs.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsEmptyBatch
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1SendMessageBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)
    private val queue = backend.create("foo")!!

    @Test
    fun `send empty batch`() {
        val exception = catchThrowableOfType(
            { client.sendMessageBatch(queue.url, listOf()) },
            AmazonSQSException::class.java
        )
        exception.assertIsEmptyBatch(SendMessageBatchRequestEntry::class.java)
    }

    @Test
    fun `send batch to missing queue`() {
        val entries = listOf(
            SendMessageBatchRequestEntry().withMessageBody("foo")
        )

        val exception = catchThrowableOfType(
            { client.sendMessageBatch("missingUrl", entries) },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `send batch`() {
        val entries = listOf(
            SendMessageBatchRequestEntry().withMessageBody("bar"),
            SendMessageBatchRequestEntry().withMessageBody("baz")
        )

        val result = client.sendMessageBatch(queue.url, entries)

        assertThat(result.failed).isEmpty()
        assertThat(result.successful).hasSize(2)
    }
}