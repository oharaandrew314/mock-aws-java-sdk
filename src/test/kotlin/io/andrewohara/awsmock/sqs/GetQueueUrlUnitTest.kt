package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GetQueueUrlUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `get url for missing queue`() {
        val exception = catchThrowableOfType({ client.getQueueUrl("foo") }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `get url for queue`() {
        val queue = client.createQueue("foo")

        assertThat(client.getQueueUrl("foo").queueUrl).isEqualTo(queue.queueUrl)
    }
}