package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.CreateTopicRequest
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV1
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertMissingParameter
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSnsV1CreateTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    @Test
    fun `create topic - without name throws exception`() {
        val exception = catchThrowableOfType(
                { client.createTopic(CreateTopicRequest()) },
                AmazonSNSException::class.java
        )

        exception.assertMissingParameter("name")
        assertThat(backend.topics()).isEmpty()
    }

    @Test
    fun `create topic`() {
        val topic = client.createTopic("foo")

        assertThat(backend.topics())
            .hasSize(1)
            .allMatch { it.arn == topic.topicArn }
            .allMatch { it.name == "foo" }
    }

    @Test
    fun `create duplicate topic - ignore`() {
        val original = client.createTopic("foo")
        val duplicate = client.createTopic("foo")

        assertThat(duplicate.topicArn).isEqualTo(original.topicArn)
        assertThat(backend.topics())
            .hasSize(1)
            .allMatch { it.name == "foo" }
            .allMatch { it.arn == original.topicArn }
    }
}
