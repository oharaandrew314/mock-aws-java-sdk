package io.andrewohara.awsmock.samples.sns

import io.andrewohara.awsmock.sns.MockAmazonSNS
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class GameServiceTest {

    private val client = MockAmazonSNS()
    private lateinit var testObj: GameService

    @Before
    fun setup() {
        val releaseTopic = client.createTopic("releaseTopic")
        val updateTopic = client.createTopic("updateTopic")

        testObj = GameService(
                client,
                releaseTopicArn = releaseTopic.topicArn,
                updateTopicArn = updateTopic.topicArn
        )
    }

    @Test
    fun `update early access game`() {
        val game = testObj.create("Satisfactory")
        testObj.update(game.id, "you can now lift things")
        testObj.update(game.id, "Now with trains and green energy!")
        testObj.update(game.id, "OMG PIPES!")

        Assertions.assertThat(testObj[game.id]).isEqualTo(Game(
                id = game.id,
                name = "Satisfactory",
                released = false,
                updates = mutableListOf(
                        "you can now lift things",
                        "Now with trains and green energy!",
                        "OMG PIPES!"
                )
        ))
    }

    @Test
    fun `release and update game`() {
        val game = testObj.create("Kingdom Come: Deliverance")
        testObj.release(game.id)
        testObj.update(game.id, "We fixed some bugs!")
        testObj.update(game.id, "We fixed some more bugs!")

        Assertions.assertThat(testObj[game.id]).isEqualTo(Game(
                id = game.id,
                name = "Kingdom Come: Deliverance",
                released = true,
                updates = mutableListOf(
                        "We fixed some bugs!",
                        "We fixed some more bugs!"
                )
        ))
    }
}