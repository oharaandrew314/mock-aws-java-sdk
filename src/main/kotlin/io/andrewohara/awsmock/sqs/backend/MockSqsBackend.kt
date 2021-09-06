package io.andrewohara.awsmock.sqs.backend

import java.time.Clock

class MockSqsBackend(private val clock: Clock = Clock.systemUTC()) {

    private val queues = mutableListOf<MockSqsQueue>()
    fun queues(namePrefix: String? = null, maxResults: Int? = null) = queues
        .asSequence()
        .filter { if (namePrefix == null) true else it.name.startsWith(namePrefix) }
        .take(maxResults ?: Int.MAX_VALUE)
        .toList()

    operator fun get(url: String) = queues.find { it.url == url }

    fun create(name: String, attributes: Map<String, String> = emptyMap()): MockSqsQueue? {
        queues.find { it.name == name}?.let { existing ->
            return if (existing.attributes == attributes) existing else null
        }

        return MockSqsQueue(
            clock = clock,
            name = name,
            url = "https://sqs.mock.aws/$name",
            attributes = attributes.toMutableMap()
        ).also { queues += it }
    }

    fun delete(url: String) = queues.removeIf { it.url == url }
}



