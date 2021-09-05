package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DeleteQueueUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `delete missing queue`() {
        val exception = catchThrowableOfType({ client.deleteQueue("https://sqs.mock.aws/missing") }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `delete queue`() {
        val queue = client.createQueue("foo")

        client.deleteQueue(queue.queueUrl)

        assertThat(client.listQueues().queueUrls).isEmpty()
    }
}