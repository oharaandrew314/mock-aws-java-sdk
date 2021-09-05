package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.InvalidParameterException
import com.amazonaws.services.sns.model.NotFoundException
import com.amazonaws.services.sns.model.PublishRequest
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsMessage
import io.andrewohara.awsmock.sns.MockSnsV1
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertInvalidParameter
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertMissingParameter
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertNotFound
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class MockSnsV1PublishTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    @Test
    fun `publish to missing topic`() {
        val exception = catchThrowableOfType(
                { client.publish("arn:aws:sns:us-east-1:583125843759:foo", "bar") },
                NotFoundException::class.java
        )

        exception.assertNotFound()
    }

    @Test
    fun `publish without arn`() {
        backend.createTopic("foo")

        val exception = catchThrowableOfType(
                { client.publish(PublishRequest().withMessage("bar")) },
                InvalidParameterException::class.java
        )

        exception.assertInvalidParameter("TopicArn or TargetArn")
    }

    @Test
    fun `publish without message`() {
        val topic = backend.createTopic("foo")

        val exception = catchThrowableOfType(
                { client.publish(PublishRequest().withTopicArn(topic.arn)) },
                AmazonSNSException::class.java
        )

        exception.assertMissingParameter("message")
    }

    @Test
    fun `publish message - without subject`() {
        val topic = backend.createTopic("foo")

        val result = client.publish(topic.arn, "bar")

        assertThat(topic.messages()).containsExactly(
            MockSnsMessage(subject = null, message = "bar", messageId = result.messageId)
        )
    }

    @Test
    fun `publish message - with subject`() {
        val topic = backend.createTopic("foo")

        val request = PublishRequest()
            .withTopicArn(topic.arn)
            .withMessage("bar")
            .withSubject("stuff")
        val result = client.publish(request)

        assertThat(topic.messages()).containsExactly(
            MockSnsMessage(subject = "stuff", message = "bar", messageId = result.messageId)
        )
    }
}