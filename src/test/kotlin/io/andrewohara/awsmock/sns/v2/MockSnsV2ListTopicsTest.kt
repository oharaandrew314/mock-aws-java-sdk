package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.Topic

class MockSnsV2ListTopicsTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    @Test
    fun `list topics - empty`() {
        assertThat(client.listTopics().topics()).isEmpty()
    }

    @Test
    fun `list topics`() {
        val foo = backend.createTopic("foo")
        val bar = backend.createTopic("bar")

        assertThat(client.listTopics().topics()).containsExactlyInAnyOrder(
                Topic.builder().topicArn(foo.arn).build(),
                Topic.builder().topicArn(bar.arn).build(),
        )
    }
}