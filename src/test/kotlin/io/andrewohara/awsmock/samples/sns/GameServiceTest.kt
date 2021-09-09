package io.andrewohara.awsmock.samples.sns

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsMessage
import io.andrewohara.awsmock.sns.MockSnsV1
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameServiceTest {
    private val mockSns = MockSnsBackend()
    private val gameEventsTopic = mockSns.createTopic("releaseTopic")
    private val testObj = GameService(MockSnsV1(mockSns), gameEventsTopic.arn)

    @Test
    fun `create game`() {
        val gameId = testObj.create("Satisfactory")

        assertThat(gameEventsTopic.messages()).containsExactly(
            MockSnsMessage(messageId = "releaseTopic:0", subject = null, message = "id=$gameId, type=create, name=Satisfactory")
        )
    }
}