package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsInvalidReceiptHandle
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class MockSqsV1DeleteMessageUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)
    private val queue = backend.create("lolcats")!!

    @Test
    fun `delete message with missing receipt handle`() {
        val exception = catchThrowableOfType(
            { client.deleteMessage(queue.url, "receiptHandle") },
            AmazonSQSException::class.java
        )
        exception.assertIsInvalidReceiptHandle()
    }

    @Test
    fun `delete message`() {
        queue.send("meow")
        val (receipt) = queue.receive()

        client.deleteMessage(queue.url, receipt.receiptHandle)

        assertThat(queue.messages).isEmpty()
    }

    @Test
    fun `delete message that's already been deleted`() {
        queue.send("meow")
        val (receipt) = queue.receive()
        queue.delete(receipt.receiptHandle)

        // delete second time with no error
        client.deleteMessage(queue.url, receipt.receiptHandle)
    }
}