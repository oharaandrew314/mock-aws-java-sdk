package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.*

class MockSqsV2SendMessageBatchUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)
    private val queue = backend.create("foo")!!

    @Test
    fun `send batch to missing queue`() {
        assertThatThrownBy {
            client.sendMessageBatch {
                it.queueUrl("missingQueue")
                it.entries(
                    SendMessageBatchRequestEntry.builder().messageBody("body").build()
                )
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `send batch`() {
        val result = client.sendMessageBatch {
            it.queueUrl(queue.url)
            it.entries(
                SendMessageBatchRequestEntry.builder().messageBody("bar").build(),
                SendMessageBatchRequestEntry.builder().messageBody("baz").build()
            )
        }

        assertThat(result.failed()).isEmpty()
        assertThat(result.successful()).hasSize(2)
    }
}