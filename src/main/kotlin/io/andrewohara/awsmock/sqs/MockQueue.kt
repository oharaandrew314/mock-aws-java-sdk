package io.andrewohara.awsmock.sqs

import java.net.URL
import java.util.*

data class MockQueue(
        val name: String,
        val url: URL,
        val attributes: Map<String, String>
) {
    val messages: Queue<MockMessage> = ArrayDeque<MockMessage>()
    val deleted = mutableListOf<MockMessage>()
}