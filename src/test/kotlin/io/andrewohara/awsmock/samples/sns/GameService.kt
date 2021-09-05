package io.andrewohara.awsmock.samples.sns

import com.amazonaws.services.sns.AmazonSNS
import java.util.*

class GameService(private val sns: AmazonSNS, private val gameEventsTopic: String) {

    private val games = mutableMapOf<UUID, String>()

    fun create(name: String): UUID {
        val id = UUID.randomUUID()
        games[id] = name

        sns.publish(gameEventsTopic, "id=$id, type=create, name=$name")

        return id
    }

    operator fun get(id: UUID): String? = games[id]
}

