package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.AmazonSNSException
import com.amazonaws.services.sns.model.CreateTopicRequest
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV1
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertMissingParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MockSnsV1CreateTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    @Test
    fun `create topic - without name throws exception`() {
        shouldThrow<AmazonSNSException> {
            client.createTopic(CreateTopicRequest())
        }.assertMissingParameter("name")

        backend.topics().shouldBeEmpty()
    }

    @Test
    fun `create topic`() {
        val topic = client.createTopic("foo")

        backend.topics()
            .shouldHaveSize(1)
            .first().should {
                it.arn shouldBe topic.topicArn
                it.name shouldBe "foo"
            }
    }

    @Test
    fun `create duplicate topic - ignore`() {
        val original = client.createTopic("foo")
        val duplicate = client.createTopic("foo")

        duplicate.topicArn shouldBe original.topicArn
        backend.topics()
            .shouldHaveSize(1)
            .first().should {
                it.arn shouldBe original.topicArn
                it.name shouldBe "foo"
            }
    }
}
