package io.andrewohara.awsmock.sns

import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.InvalidParameterException
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.PublishRequest
import io.andrewohara.awsmock.sns.SnsUtils.assertInvalidParameter
import io.andrewohara.awsmock.sns.SnsUtils.assertMissingParameter
import io.andrewohara.awsmock.sns.SnsUtils.assertNotFound
import org.assertj.core.api.Assertions
import org.junit.Test

class PublishTest {

    private val client = MockAmazonSNS()

    @Test
    fun `publish to missing topic`() {
        val exception = Assertions.catchThrowableOfType(
                { client.publish("arn:aws:sns:us-east-1:583125843759:foo", "bar") },
                NotFoundException::class.java
        )

        exception.assertNotFound()
    }

    @Test
    fun `publish without arn`() {
        client.createTopic("foo")

        val exception = Assertions.catchThrowableOfType(
                { client.publish(PublishRequest().withMessage("bar")) },
                InvalidParameterException::class.java
        )

        exception.assertInvalidParameter("TopicArn or TargetArn")
    }

    @Test
    fun `publish without message`() {
        val topic = client.createTopic("foo")

        val exception = Assertions.catchThrowableOfType(
                { client.publish(PublishRequest().withTopicArn(topic.topicArn)) },
                AmazonSNSException::class.java
        )

        exception.assertMissingParameter("message")
    }

    @Test
    fun `publish message`() {
        val topic = client.createTopic("foo")

        val result = client.publish(topic.topicArn, "bar")

        Assertions.assertThat(result.messageId).isNotEmpty()
    }
}