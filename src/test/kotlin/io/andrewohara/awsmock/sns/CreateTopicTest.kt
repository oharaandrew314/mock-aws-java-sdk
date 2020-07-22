package io.andrewohara.awsmock.sns

import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.CreateTopicRequest
import io.andrewohara.awsmock.sns.SnsUtils.assertMissingParameter
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Test

class CreateTopicTest {

    private val client = MockAmazonSNS()

    @After
    fun cleanup() {
        client.deleteTopic("arn:aws:sns:us-east-1:583125843759:foo")
    }

    @Test
    fun `create topic without name`() {
        val exception = catchThrowableOfType(
                { client.createTopic(CreateTopicRequest()) },
                AmazonSNSException::class.java
        )

        exception.assertMissingParameter("name")
        assertThat(client.listTopics().topics).isEmpty()
    }

    @Test
    fun `create topic`() {
        val topic = client.createTopic("foo")

        assertThat(client.listTopics().topics.map { it.topicArn }).containsExactly(topic.topicArn)
    }

    @Test
    fun `create duplicate topic`() {
        val original = client.createTopic("foo")
        val duplicate = client.createTopic("foo")

        assertThat(duplicate.topicArn).isEqualTo(original.topicArn)
        assertThat(client.listTopics().topics.map { it.topicArn }).containsExactly(original.topicArn)
    }
}
