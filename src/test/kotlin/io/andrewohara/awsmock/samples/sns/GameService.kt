package io.andrewohara.awsmock.samples.sns

import com.amazonaws.services.sns.AmazonSNS
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.lang.IllegalArgumentException

data class Game(
        val id: Int,
        val name: String,
        var released: Boolean = false,
        val updates: MutableList<String> = mutableListOf()
)

class GameService(private val sns: AmazonSNS, private val releaseTopicArn: String, private val updateTopicArn: String) {

    private val mapper = jacksonObjectMapper()
    private val games = mutableMapOf<Int, Game>()

    operator fun get(id: Int): Game? = games[id]

    fun create(name: String): Game {
        val game = Game(id = games.size + 1, name = name)
        games[game.id] = game
        return game
    }

    fun release(id: Int) {
        val game = games[id] ?: throw IllegalArgumentException("Game not found: $id")
        game.released = true

        sns.publish(releaseTopicArn, mapper.writeValueAsString(game))
    }

    fun update(id: Int, notes: String) {
        val game = games[id] ?: throw IllegalArgumentException("Game not found: $id")
        game.updates.add(notes)

        sns.publish(updateTopicArn, mapper.writeValueAsString(game))
    }
}

