package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.CreateQueueRequest
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsInvalidParameter
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueNameAlreadyExists
import org.assertj.core.api.Assertions.*
import org.junit.Test

class CreateQueueUnitTest {

    private val client = MockAmazonSQS()

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
    fun `create identical queue that already exists`() {
        val original = client.createQueue("stuff")

        val duplicate = client.createQueue("stuff")

        assertThat(duplicate.queueUrl).isEqualTo(original.queueUrl)
        assertThat(client.listQueues().queueUrls).hasSize(1)  // should not duplicate queue
    }

    @Test
    fun `create queue (with different attributes) that already exists`() {
        val request = CreateQueueRequest("stuff").withAttributes(mapOf("VisibilityTimeout" to "9001"))
        client.createQueue(request)

        val exception = catchThrowableOfType({ client.createQueue("stuff") }, AmazonSQSException::class.java)
        exception.assertIsQueueNameAlreadyExists()
    }
}