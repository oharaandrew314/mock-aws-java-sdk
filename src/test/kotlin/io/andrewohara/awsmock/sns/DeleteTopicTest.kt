package io.andrewohara.awsmock.sns

import com.amazonaws.services.sns.model.DeleteTopicRequest
import com.amazonaws.services.sns.model.InvalidParameterException
import io.andrewohara.awsmock.sns.SnsUtils.assertInvalidParameter
import org.assertj.core.api.Assertions
import org.junit.Test

class DeleteTopicTest {

    private val client = MockAmazonSNS()

    @Test
    fun `delete with null arn`() {
        val exception = Assertions.catchThrowableOfType(
                { client.deleteTopic(DeleteTopicRequest()) },
                InvalidParameterException::class.java
        )

        exception.assertInvalidParameter("TopicArn")
    }

    @Test
    fun `delete missing topic`() {
        client.deleteTopic("arn:mockaws:sns:region:account-id:foo")
    }

    @Test
    fun `delete topic`() {
        val topic = client.createTopic("foo")

        client.deleteTopic(topic.topicArn)

        Assertions.assertThat(client.listTopics().topics).isEmpty()
    }

    @Test
    fun `delete deleted`() {
        val topic = client.createTopic("foo")

        client.deleteTopic(topic.topicArn)

        client.deleteTopic(topic.topicArn)

        Assertions.assertThat(client.listTopics().topics).isEmpty()
    }
}