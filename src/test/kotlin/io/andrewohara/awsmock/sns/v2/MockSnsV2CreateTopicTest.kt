package io.andrewohara.awsmock.sns.v2

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.SnsException

class MockSnsV2CreateTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV2(backend)

    @Test
    fun `create topic - without name throws exception`() {
        shouldThrow<SnsException> {
            client.createTopic { }
        }
    }

    @Test
    fun `create topic`() {
        val topic = client.createTopic {
            it.name("foo")
        }

        backend.topics()
            .shouldHaveSize(1)
            .first()
            .let {
                it.arn shouldBe topic.topicArn()
                it.name shouldBe "foo"
            }
    }

    @Test
    fun `create duplicate topic - ignore`() {
        val original = client.createTopic {
            it.name("foo")
        }
        val duplicate = client.createTopic {
            it.name("foo")
        }

        duplicate.topicArn() shouldBe original.topicArn()

        backend.topics()
            .shouldHaveSize(1)
            .first()
            .let {
                it.arn shouldBe original.topicArn()
                it.name shouldBe "foo"
            }
    }
}