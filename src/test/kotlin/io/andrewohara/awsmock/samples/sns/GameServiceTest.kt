package io.andrewohara.awsmock.samples.sns

import io.andrewohara.awsmock.sns.MockSnsBackend
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

        assertThat(gameEventsTopic.messages())
            .hasSize(1)
            .extracting<String> { it.message }.containsExactly("id=$gameId, type=create, name=Satisfactory")
    }
}