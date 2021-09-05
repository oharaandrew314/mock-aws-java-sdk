package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.Topic
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MockSnsV1ListTopicsTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    @Test
    fun `list topics - empty`() {
        assertThat(client.listTopics().topics).isEmpty()
    }

    @Test
    fun `list topics`() {
        val foo = backend.createTopic("foo")
        val bar = backend.createTopic("bar")

        assertThat(client.listTopics().topics).containsExactlyInAnyOrder(
                Topic().withTopicArn(foo.arn),
                Topic().withTopicArn(bar.arn)
        )
    }
}