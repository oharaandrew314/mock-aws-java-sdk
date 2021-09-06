package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse

class MockSqsV2ListQueuesUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)

    @Test
    fun `list queues when there are none`() {
        assertThat(client.listQueues()).isEqualTo(
            ListQueuesResponse
                .builder()
                .queueUrls(emptyList())
                .build()
        )
    }

    @Test
    fun `list queues`() {
        val queue1 = backend.create("queue1")!!
        val queue2 = backend.create("queue2")!!

        assertThat(client.listQueues()).isEqualTo(
            ListQueuesResponse
                .builder()
                .queueUrls(queue1.url, queue2.url)
                .build()
        )
    }

    @Test
    fun `list queues while filtering by prefix`() {
        val targetQueue = backend.create("cats-meows")!!
        backend.create("dogs-barks")

        val response = client.listQueues {
            it.queueNamePrefix("cats")
        }
        assertThat(response).isEqualTo(
            ListQueuesResponse.builder()
                .queueUrls(targetQueue.url)
                .build()
        )
    }
}