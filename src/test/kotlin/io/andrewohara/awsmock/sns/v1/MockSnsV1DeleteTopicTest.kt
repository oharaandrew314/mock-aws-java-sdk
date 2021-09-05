package io.andrewohara.awsmock.sns.v1

import com.amazonaws.services.sns.model.DeleteTopicRequest
import com.amazonaws.services.sns.model.InvalidParameterException
import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV1
import io.andrewohara.awsmock.sns.v1.SnsUtils.assertInvalidParameter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class MockSnsV1DeleteTopicTest {

    private val backend = MockSnsBackend()
    private val client = MockSnsV1(backend)

    @Test
    fun `delete with null arn`() {
        val exception = catchThrowableOfType(
                { client.deleteTopic(DeleteTopicRequest()) },
                InvalidParameterException::class.java
        )

        exception.assertInvalidParameter("TopicArn")
    }

    @Test
    fun `delete missing topic`() {
        client.deleteTopic("arn:mockaws:sns:region:account-id:foo")  // no error
    }

    @Test
    fun `delete topic`() {
        val topic = backend.createTopic("foo")

        client.deleteTopic(topic.arn)

        assertThat(backend.topics()).isEmpty()
    }
}