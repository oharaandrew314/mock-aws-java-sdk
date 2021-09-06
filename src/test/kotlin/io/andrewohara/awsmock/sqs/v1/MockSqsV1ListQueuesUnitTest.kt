package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.ListQueuesRequest
import com.amazonaws.services.sqs.model.ListQueuesResult
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1ListQueuesUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)

    @Test
    fun `list queues when there are none`() {
        assertThat(client.listQueues()).isEqualTo(
            ListQueuesResult()
                .withQueueUrls(emptyList())
        )
    }

    @Test
    fun `list queues`() {
        val queue1 = backend.create("queue1")!!
        val queue2 = backend.create("queue2")!!

        assertThat(client.listQueues()).isEqualTo(
            ListQueuesResult()
                .withQueueUrls(queue1.url, queue2.url)
        )
    }

    @Test
    fun `list queues while filtering by prefix`() {
        val targetQueue = backend.create("cats-meows")!!
        backend.create("dogs-barks")

        val request = ListQueuesRequest().withQueueNamePrefix("cats")
        assertThat(client.listQueues(request)).isEqualTo(
            ListQueuesResult()
                .withQueueUrls(targetQueue.url)
        )
    }
}