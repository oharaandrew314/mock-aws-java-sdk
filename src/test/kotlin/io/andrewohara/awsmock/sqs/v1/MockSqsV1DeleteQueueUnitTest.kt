package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1DeleteQueueUnitTest {

    private val backend = MockSqsBackend()
    private val testObj = MockSqsV1(backend)

    @Test
    fun `delete missing queue`() {
        val exception = catchThrowableOfType(
            { testObj.deleteQueue("https://sqs.mock.aws/missing") },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `delete queue`() {
        val queue = backend.create("foo")!!

        testObj.deleteQueue(queue.url)

        assertThat(backend.queues()).isEmpty()
    }
}