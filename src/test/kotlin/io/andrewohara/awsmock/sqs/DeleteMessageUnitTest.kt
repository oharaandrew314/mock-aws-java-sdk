package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsInvalidReceiptHandle
import io.andrewohara.awsmock.sqs.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions
import org.junit.Test

class DeleteMessageUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `delete message with valid receipt handle but wrong queue`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        val exception = Assertions.catchThrowableOfType({ client.deleteMessage("missingUrl", received.receiptHandle) }, AmazonSQSException::class.java)
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `delete message with missing receipt handle`() {
        val queue = client.createQueue("lolcats")

        val exception = Assertions.catchThrowableOfType({ client.deleteMessage(queue.queueUrl, "receiptHandle") }, AmazonSQSException::class.java)
        exception.assertIsInvalidReceiptHandle()
    }

    @Test
    fun `delete message`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        client.deleteMessage(queue.queueUrl, received.receiptHandle)

        Assertions.assertThat(client[queue.queueUrl]?.messages).isEmpty()
    }

    @Test
    fun `delete message that's already been deleted`() {
        val queue = client.createQueue("lolcats")
        client.sendMessage(queue.queueUrl, "meow")
        val received = client.receiveMessage(queue.queueUrl).messages.first()

        // delete first time
        client.deleteMessage(queue.queueUrl, received.receiptHandle)

        // delete second time with no error
        client.deleteMessage(queue.queueUrl, received.receiptHandle)
    }
}