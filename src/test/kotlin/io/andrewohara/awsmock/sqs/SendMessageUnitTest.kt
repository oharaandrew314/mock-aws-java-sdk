package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.SendMessageRequest
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.Test

class SendMessageUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `send message to missing queue`() {
        val exception = catchThrowableOfType({ client.sendMessage("missingQueue", "foo") }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `send message`() {
        val queue = client.createQueue("lolcats")

        val result = client.sendMessage(queue.queueUrl, "meow")

        assertThat(result.messageId).isNotNull().isNotEmpty()
        assertThat(client[queue.queueUrl]!!.messages).containsExactly(
                MockMessage(
                        id = result.messageId,
                        body = "meow"
                )
        )
    }

    @Test
    fun `send message with delay`() {
        val queue = client.createQueue("lolcats")

        val request = SendMessageRequest()
                .withMessageBody("meow")                .withQueueUrl(queue.queueUrl)

                .withDelaySeconds(30)
        val result = client.sendMessage(request)

        assertThat(result.messageId).isNotNull().isNotEmpty()
        assertThat(client[queue.queueUrl]!!.messages).containsExactly(
                MockMessage(
                        id = result.messageId,
                        body = "meow"
                )
        )
    }
}