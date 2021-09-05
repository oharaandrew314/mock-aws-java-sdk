package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV2
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.SnsException

class MockSnsV2DeleteTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    @Test
    fun `delete with null arn`() {
        assertThatThrownBy { client.deleteTopic { } }
            .isInstanceOf(SnsException::class.java)
    }

    @Test
    fun `delete missing topic`() {
        client.deleteTopic {
            it.topicArn("arn:mockaws:sns:region:account-id:foo")
        }
    }

    @Test
    fun `delete topic`() {
        val topic = backend.createTopic("foo")

        client.deleteTopic {
            it.topicArn(topic.arn)
        }

        assertThat(backend.topics()).isEmpty()
    }
}