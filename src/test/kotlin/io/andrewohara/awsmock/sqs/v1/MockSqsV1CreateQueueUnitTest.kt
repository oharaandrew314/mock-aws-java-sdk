package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.CreateQueueRequest
import io.andrewohara.awsmock.sqs.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsInvalidParameter
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueNameAlreadyExists
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class MockSqsV1CreateQueueUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)

    @Test
    fun `create queue`() {
        val queue = client.createQueue("stuff")
        assertThat(queue.queueUrl).isEqualTo("https://sqs.mock.aws/stuff")
    }

    @Test
    fun `create queue without name`() {
        val request = CreateQueueRequest().withAttributes(mapOf("VisibilityTimeout" to "9001"))

        val exception = catchThrowableOfType({ client.createQueue(request) }, AmazonSQSException::class.java)

        exception.assertIsInvalidParameter()
    }

    @Test
    fun `create queue with visibility timeout and delay`() {
        val request = CreateQueueRequest()
            .withQueueName("lolcats")
            .withAttributes(mapOf("VisibilityTimeout" to "9001", "DelaySeconds" to "30"))

        val result = client.createQueue(request)
        assertThat(result.queueUrl).isEqualTo("https://sqs.mock.aws/lolcats")

        val backendQueue = backend[result.queueUrl]
        assertThat(backendQueue).isNotNull
        assertThat(backendQueue?.defaultVisibilityTimeout).isEqualTo(Duration.ofSeconds(9001))
        assertThat(backendQueue?.defaultDelay).isEqualTo(Duration.ofSeconds(30))
    }

    @Test
    fun `create identical queue that already exists`() {
        val original = backend.create("stuff")!!

        val duplicate = client.createQueue("stuff")

        assertThat(duplicate.queueUrl).isEqualTo(original.url)
        assertThat(client.listQueues().queueUrls).hasSize(1)  // should not duplicate queue
    }

    @Test
    fun `create queue (with different attributes) that already exists`() {
        backend.create("stuff", mapOf("foo" to "bar"))

        val exception = catchThrowableOfType(
            { client.createQueue("stuff") },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueNameAlreadyExists()
    }
}