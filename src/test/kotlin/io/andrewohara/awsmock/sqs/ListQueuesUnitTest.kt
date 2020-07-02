package io.andrewohara.awsmock.sqs

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ListQueuesUnitTest {

    private val client = MockAmazonSQS()

    @Test
    fun `list queues when there are none`() {
        val result = client.listQueues()
        assertThat(result.queueUrls).isEmpty()
    }

    @Test
    fun `list queues`() {
        val queue1 = client.createQueue("queue1")
        val queue2 = client.createQueue("queue2")

        assertThat(client.listQueues().queueUrls).containsExactlyInAnyOrder(
                queue1.queueUrl,
                queue2.queueUrl
        )
    }

    @Test
    fun `list queues while filtering by prefix`() {
        val targetQueue = client.createQueue("cats-meows")
        client.createQueue("dogs-barks")

        assertThat(client.listQueues("cats").queueUrls)
                .containsExactly(targetQueue.queueUrl)
    }
}