package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsMessage
import io.andrewohara.awsmock.sns.MockSnsV2
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.NotFoundException
import software.amazon.awssdk.services.sns.model.SnsException

class MockSnsV2PublishTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    @Test
    fun `publish to missing topic`() {
        assertThatThrownBy { client.publish {
            it.topicArn("arn:aws:sns:us-east-1:583125843759:foo")
            it.message("bar")
        } }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `publish without arn`() {
        backend.createTopic("foo")

        assertThatThrownBy { client.publish {
            it.message("bar")
        } }.isInstanceOf(SnsException::class.java)
    }

    @Test
    fun `publish without message`() {
        val topic = backend.createTopic("foo")

        assertThatThrownBy { client.publish {
            it.topicArn(topic.arn)
        } }.isInstanceOf(SnsException::class.java)
    }

    @Test
    fun `publish message - without subject`() {
        val topic = backend.createTopic("foo")

        val result = client.publish {
            it.topicArn(topic.arn)
            it.message("bar")
        }

        assertThat(topic.messages()).containsExactly(
            MockSnsMessage(subject = null, message = "bar", messageId = result.messageId())
        )
    }

    @Test
    fun `publish message - with subject`() {
        val topic = backend.createTopic("foo")

        val result = client.publish {
            it.topicArn(topic.arn)
            it.message("bar")
            it.subject("stuff")
        }

        assertThat(topic.messages()).containsExactly(
            MockSnsMessage(subject = "stuff", message = "bar", messageId = result.messageId())
        )
    }
}