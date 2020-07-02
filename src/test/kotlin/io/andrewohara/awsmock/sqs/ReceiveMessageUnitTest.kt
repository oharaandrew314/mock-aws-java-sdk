package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ReceiveMessageUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `receive message from missing queue`() {
        val exception = catchThrowableOfType({ client.receiveMessage("missingUrl") }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `receive message from empty queue`() {
        val queue = client.createQueue("lolcats")

        val result = client.receiveMessage(queue.queueUrl)

        assertThat(result.messages).isEmpty()
    }

    @Test
    fun `receive single message from queue`() {
        val queue = client.createQueue("lolcats")
        val sent = client.sendMessage(queue.queueUrl, "meow")

        val result = client.receiveMessage(queue.queueUrl)
        assertThat(result.messages).hasSize(1)

        val received = result.messages.first()
        assertThat(received.messageId).isNotNull().isEqualTo(sent.messageId)
        assertThat(received.body).isEqualTo("meow")
        assertThat(received.receiptHandle).isNotEmpty()
    }

    @Test
    fun `receive message while only message has already been received`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        client.receiveMessage(queue.queueUrl)

        val result = client.receiveMessage(queue.queueUrl)

        assertThat(result.messages).isEmpty()
    }

    @Test
    fun `receive multiple messages from queue enough times to exhaust it after multiple receives`() {
        val queue = client.createQueue("lolcats")
        repeat(4) { client.sendMessage(queue.queueUrl, "message$it") }

        val result1 = client.receiveMessage(ReceiveMessageRequest().withQueueUrl(queue.queueUrl).withMaxNumberOfMessages(2))
        assertThat(result1.messages).hasSize(2)

        val result2 = client.receiveMessage(ReceiveMessageRequest().withQueueUrl(queue.queueUrl).withMaxNumberOfMessages(2))
        assertThat(result2.messages).hasSize(2)

        val result3 = client.receiveMessage(ReceiveMessageRequest().withQueueUrl(queue.queueUrl).withMaxNumberOfMessages(2))
        assertThat(result3.messages).isEmpty()
    }
}