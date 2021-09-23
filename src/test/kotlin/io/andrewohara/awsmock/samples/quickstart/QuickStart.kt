package io.andrewohara.awsmock.samples.quickstart

import io.andrewohara.awsmock.sns.MockSnsBackend
import io.andrewohara.awsmock.sns.MockSnsV2
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.SnsClient
import java.util.*

class GameService(private val sns: SnsClient, private val eventsTopicArn: String) {

    private val gamesDao = mutableMapOf<UUID, String>()

    operator fun get(id: UUID) = gamesDao[id]

    fun createGame(name: String): UUID {
        val id = UUID.randomUUID()
        gamesDao[id] = name

        sns.publish {
            it.topicArn(eventsTopicArn)
            it.message(name)
        }

        return id
    }

    fun startServer() {
        // do stuff
    }
}

// inject a real client for real use
fun main(args: Array<String>) {
    val eventsTopicArn = args.first()
    val sns = SnsClient.create()
    val service = GameService(sns, eventsTopicArn)
    service.startServer()
}

class GameServiceTest {

    private val backend = MockSnsBackend()
    private val topic = backend.createTopic("game-events")

    // inject a mock client for tests
    private val testObj = GameService(
        sns = MockSnsV2(backend),
        eventsTopicArn = topic.arn
    )

    @Test
    fun `create game`() {
        val id = testObj.createGame("Mass Effect 3")
        Assertions.assertThat(testObj[id]).isEqualTo("Mass Effect 3")
        Assertions.assertThat(topic.messages().map { it.message }).containsExactly("Mass Effect 3")
    }
}