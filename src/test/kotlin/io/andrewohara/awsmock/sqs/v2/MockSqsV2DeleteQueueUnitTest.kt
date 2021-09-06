package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

class MockSqsV2DeleteQueueUnitTest {

    private val backend = MockSqsBackend()
    private val testObj = MockSqsV2(backend)

    @Test
    fun `delete missing queue`() {
        assertThatThrownBy {
            testObj.deleteQueue {
                it.queueUrl("https://sqs.mock.aws/missing")
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `delete queue`() {
        val queue = backend.create("foo")!!

        val response = testObj.deleteQueue {
            it.queueUrl(queue.url)
        }

        assertThat(response).isEqualTo(DeleteQueueResponse.builder().build())
        assertThat(backend.queues()).isEmpty()
    }
}