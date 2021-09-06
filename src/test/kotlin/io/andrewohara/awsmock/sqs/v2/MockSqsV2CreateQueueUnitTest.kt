package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException
import java.lang.NullPointerException
import java.time.Duration

class MockSqsV2CreateQueueUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV2(backend)

    @Test
    fun `create queue`() {
        val queue = client.createQueue {
            it.queueName("stuff")
        }

        assertThat(queue).isEqualTo(
            CreateQueueResponse.builder()
                .queueUrl("https://sqs.mock.aws/stuff")
                .build()
        )
    }

    @Test
    fun `create queue without name`() {
        assertThatThrownBy {
            client.createQueue {
                it.attributesWithStrings(mapOf("VisibilityTimeout" to "30"))
            }
        }.isInstanceOf(NullPointerException::class.java) // technically this should be a fancy sqs exception, but it's the SDKs fault for allowing it at all
    }

    @Test
    fun `create queue with visibility timeout and delay`() {
        val result = client.createQueue {
            it.queueName("lolcats")
            it.attributesWithStrings(mapOf("VisibilityTimeout" to "9001", "DelaySeconds" to "30"))
        }

        assertThat(result).isEqualTo(
            CreateQueueResponse.builder().queueUrl("https://sqs.mock.aws/lolcats").build()
        )

        val backendQueue = backend[result.queueUrl()]
        assertThat(backendQueue).isNotNull
        assertThat(backendQueue?.defaultVisibilityTimeout()).isEqualTo(Duration.ofSeconds(9001))
        assertThat(backendQueue?.defaultDelay()).isEqualTo(Duration.ofSeconds(30))
    }

    @Test
    fun `create identical queue that already exists`() {
        val original = backend.create("stuff")!!

        val duplicate = client.createQueue {
            it.queueName(original.name)
        }

        assertThat(duplicate.queueUrl()).isEqualTo(original.url)
        assertThat(backend.queues()).hasSize(1)  // should not duplicate queue
    }

    @Test
    fun `create queue (with different attributes) that already exists`() {
        backend.create("stuff", mapOf("foo" to "bar"))

        assertThatThrownBy {
            client.createQueue {
                it.queueName("stuff")
            }
        }.isInstanceOf(QueueNameExistsException::class.java)
    }
}