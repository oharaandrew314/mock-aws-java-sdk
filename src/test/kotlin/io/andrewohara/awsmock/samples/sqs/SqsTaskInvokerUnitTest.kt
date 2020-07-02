package io.andrewohara.awsmock.samples.sqs

import io.andrewohara.awsmock.sqs.MockAmazonSQS
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class SqsTaskInvokerUnitTest {

    private val sqsClient = MockAmazonSQS()
    private lateinit var queueUrl: String
    private lateinit var testObj: SqsTaskInvoker

    @Before
    fun setup() {
        queueUrl = sqsClient.createQueue("cat-demands").queueUrl
        testObj = SqsTaskInvoker(queueUrl, sqsClient)
    }

    @Test
    fun meow() {
        testObj.meow()

        Assertions.assertThat(sqsClient[queueUrl]!!.messages).hasSize(1)
    }
}