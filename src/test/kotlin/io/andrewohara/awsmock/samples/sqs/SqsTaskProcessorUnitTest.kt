package io.andrewohara.awsmock.samples.sqs

import io.andrewohara.awsmock.sqs.MockAmazonSQS
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class SqsTaskProcessorUnitTest {

    private val cat = Cat()
    private val sqsClient = MockAmazonSQS()
    private lateinit var queue: String
    private lateinit var testObj: SqsTaskProcessor

    @Before
    fun setup() {
        queue = sqsClient.createQueue("cat-commands").queueUrl
        testObj = SqsTaskProcessor(cat, queue, sqsClient)
    }

    @Test
    fun processMeow() {
        sqsClient.sendMessage(queue, "meow")

        testObj.processNextTask()

        Assertions.assertThat(cat.actions).containsExactly("I tell her it's not dinner time yet")
    }
}