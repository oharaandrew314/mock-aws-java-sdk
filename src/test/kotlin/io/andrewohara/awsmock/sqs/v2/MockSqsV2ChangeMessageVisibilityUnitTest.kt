package io.andrewohara.awsmock.sqs.v2

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException
import software.amazon.awssdk.services.sqs.model.SqsException

class MockSqsV2ChangeMessageVisibilityUnitTest {

    private val backend = MockSqsBackend()
    private val queue = backend.create("foo")!!.also {
        it.send("foo")
    }
    private val client = MockSqsV2(backend)

    private val receipt = queue.receive().first()

    @Test
    fun `change visibility for invalid receipt handle`() {
        assertThatThrownBy {
            client.changeMessageVisibility {
                it.queueUrl(queue.url)
                it.receiptHandle("invalidHandle")
                it.visibilityTimeout(30)
            }
        }.isInstanceOf(ReceiptHandleIsInvalidException::class.java)
    }

    @Test
    fun `change visibility for missing queue`() {
        assertThatThrownBy {
            client.changeMessageVisibility {
                it.queueUrl("missingQueue")
                it.receiptHandle(receipt.receiptHandle)
                it.visibilityTimeout(30)
            }
        }.isInstanceOf(QueueDoesNotExistException::class.java)
    }

    @Test
    fun `change visibility to negative amount`() {
        assertThatThrownBy {
            client.changeMessageVisibility {
                it.queueUrl(queue.url)
                it.receiptHandle(receipt.receiptHandle)
                it.visibilityTimeout(-1)
            }
        }.isInstanceOf(SqsException::class.java)
    }

    @Test
    fun `change visibility to max int`() {
        assertThatThrownBy {
            client.changeMessageVisibility {
                it.queueUrl(queue.url)
                it.receiptHandle(receipt.receiptHandle)
                it.visibilityTimeout(Int.MAX_VALUE)
            }
        }.isInstanceOf(SqsException::class.java)
    }

    @Test
    fun `change visibility to 20 seconds`() {
        val result = client.changeMessageVisibility {
            it.queueUrl(queue.url)
            it.receiptHandle(receipt.receiptHandle)
            it.visibilityTimeout(20)
        }

        assertThat(result).isEqualTo(
            ChangeMessageVisibilityResponse.builder().build()
        )
    }
}