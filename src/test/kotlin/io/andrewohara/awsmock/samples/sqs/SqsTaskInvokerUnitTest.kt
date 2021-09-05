package io.andrewohara.awsmock.samples.sqs

import io.andrewohara.awsmock.sqs.MockAmazonSQS
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class SqsTaskInvokerUnitTest {

    private val sqsClient = MockAmazonSQS()
    private val queueUrl = sqsClient.createQueue("cat-demands").queueUrl
    private val testObj = SqsTaskInvoker(queueUrl, sqsClient)

    @Test
    fun meow() {
        testObj.meow()

        Assertions.assertThat(sqsClient[queueUrl]!!.messages).hasSize(1)
    }
}