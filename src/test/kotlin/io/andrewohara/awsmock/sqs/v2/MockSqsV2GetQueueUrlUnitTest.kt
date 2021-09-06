package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

class MockSqsV2GetQueueUrlUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)

    @Test
    fun `get url for missing queue`() {
        assertThatThrownBy {
            client.getQueueUrl {
                it.queueName("foo")
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `get url for queue`() {
        val foo  = backend.create("foo")!!
        val bar = backend.create("bar")!!

        assertThat(client.getQueueUrl { it.queueName(foo.name) })
            .isEqualTo(GetQueueUrlResponse.builder().queueUrl(foo.url).build())
        assertThat(client.getQueueUrl { it.queueName(bar.name) })
            .isEqualTo(GetQueueUrlResponse.builder().queueUrl(bar.url).build())
    }
}