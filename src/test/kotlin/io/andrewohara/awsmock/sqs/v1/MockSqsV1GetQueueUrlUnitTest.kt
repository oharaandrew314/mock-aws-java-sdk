package io.andrewohara.awsmock.sqs.v1

import com.amazonaws.services.sqs.model.AmazonSQSException
import io.andrewohara.awsmock.sqs.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV1
import io.andrewohara.awsmock.sqs.v1.SQSAssertions.assertIsQueueDoesNotExist
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSqsV1GetQueueUrlUnitTest {

    private val backend = MockSqsBackend()
    private val client = MockSqsV1(backend)

    @Test
    fun `get url for missing queue`() {
        val exception = catchThrowableOfType(
            { client.getQueueUrl("foo") },
            AmazonSQSException::class.java
        )
        exception.assertIsQueueDoesNotExist()
    }

    @Test
    fun `get url for queue`() {
        val foo  = backend.create("foo")!!
        val bar = backend.create("bar")!!

        assertThat(client.getQueueUrl("foo").queueUrl).isEqualTo(foo.url)
        assertThat(client.getQueueUrl("bar").queueUrl).isEqualTo(bar.url)
    }
}