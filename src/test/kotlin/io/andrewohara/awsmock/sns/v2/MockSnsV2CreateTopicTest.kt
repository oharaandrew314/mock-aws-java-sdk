package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV2
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.SnsException

class MockSnsV2CreateTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    @Test
    fun `create topic - without name throws exception`() {
        assertThatThrownBy { client.createTopic { } }
            .isInstanceOf(SnsException::class.java)

        assertThat(backend.topics()).isEmpty()
    }

    @Test
    fun `create topic`() {
        val topic = client.createTopic {
            it.name("foo")
        }

        assertThat(backend.topics())
            .hasSize(1)
            .allMatch { it.arn == topic.topicArn() }
            .allMatch { it.name == "foo" }
    }

    @Test
    fun `create duplicate topic - ignore`() {
        val original = client.createTopic {
            it.name("foo")
        }
        val duplicate = client.createTopic {
            it.name("foo")
        }

        assertThat(duplicate.topicArn()).isEqualTo(original.topicArn())
        assertThat(backend.topics())
            .hasSize(1)
            .allMatch { it.name == "foo" }
            .allMatch { it.arn == original.topicArn() }
    }
}