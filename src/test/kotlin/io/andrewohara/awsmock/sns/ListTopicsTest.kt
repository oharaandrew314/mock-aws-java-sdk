package io.andrewohara.awsmock.sns

import com.amazonaws.services.sns.model.Topic
import org.assertj.core.api.Assertions
import org.junit.Test

class ListTopicsTest {

    private val client = MockAmazonSNS()

    @Test
    fun `list topics`() {
        val foo = client.createTopic("foo")
        val bar = client.createTopic("bar")

        Assertions.assertThat(client.listTopics().topics).containsExactlyInAnyOrder(
                Topic().withTopicArn(foo.topicArn),
                Topic().withTopicArn(bar.topicArn)
        )
    }
}