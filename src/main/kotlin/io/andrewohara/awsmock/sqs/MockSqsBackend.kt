package io.andrewohara.awsmock.sqs

import java.nio.ByteBuffer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

class MockSqsBackend(private val clock: Clock = Clock.systemUTC()) {

    private val queues = mutableListOf<Queue>()
    fun queues(namePrefix: String? = null) = queues
        .filter { if (namePrefix == null) true else it.name.startsWith(namePrefix) }
        .toList()

    operator fun get(url: String) = queues.find { it.url == url }

    fun create(name: String, attributes: Map<String, String> = emptyMap()): Queue? {
        queues.find { it.name == name}?.let { existing ->
            return if (existing.attributes == attributes) existing else null
        }

        return Queue(
            name = name,
            url = "https://sqs.mock.aws/$name",
            attributes = attributes.toMap()
        ).also { queues += it }
    }

    fun delete(url: String) = queues.removeIf { it.url == url }

    inner class Queue(
        val name: String,
        val url: String,
        val attributes: Map<String, String>
    ) {
        val messages = mutableListOf<MockSqsMessage>()
        private val receipts = mutableListOf<MockSqsReceipt>()

        val defaultVisibilityTimeout: Duration = attributes["VisibilityTimeout"]
            ?.let { Duration.ofSeconds(it.toLong()) }
            ?: Duration.ofSeconds(30)
        val defaultDelay: Duration = attributes["DelaySeconds"]
            ?.let { Duration.ofSeconds(it.toLong()) }
            ?: Duration.ZERO

        fun send(body: String, delay: Duration? = null, attributes: Map<String, MockSqsAttribute> = emptyMap()) = MockSqsMessage(
            id = UUID.randomUUID().toString(),
            body = body,
            attributes = attributes.toMap(),
            visibleAt = clock.instant() + (delay ?: defaultDelay)
        ).also { messages += it }

        fun receive(limit: Int = Int.MAX_VALUE, visibilityTimeout: Duration? = null): List<MockSqsReceipt> {
            val time = clock.instant()
            return messages
                .asSequence()
                .filter { time >= it.visibleAt }
                .take(limit)
                .map { message ->
                    message.visibleAt = time + (visibilityTimeout ?: defaultVisibilityTimeout)
                    MockSqsReceipt(UUID.randomUUID().toString(), message)
                        .also { receipts += it }
                }
                .toList()
        }

        /**
         * True if receiptHandle existed at some point; the message could have already been deleted
         */
        fun delete(receiptHandle: String): Boolean {
            val receipt = receipts.find { it.receiptHandle == receiptHandle } ?: return false
            messages.remove(receipt.message)
            return true
        }

        fun updateVisibilityTimeout(receiptHandle: String, timeout: Duration): MockSqsUpdateVisibilityResult {
            val receipt = receipts.find { it.receiptHandle == receiptHandle } ?: return MockSqsUpdateVisibilityResult.NotFound
            if (timeout.isNegative) return MockSqsUpdateVisibilityResult.InvalidTimeout

            receipt.message.visibleAt = clock.instant() + timeout

            return MockSqsUpdateVisibilityResult.Updated
        }
    }
}

enum class MockSqsUpdateVisibilityResult { Updated, NotFound, InvalidTimeout }

data class MockSqsReceipt(
    val receiptHandle: String,
    val message: MockSqsMessage
)

data class MockSqsMessage(
    val id: String,
    val body: String,
    val attributes: Map<String, MockSqsAttribute>,
    var visibleAt: Instant
)

sealed class MockSqsAttribute {
    data class Text(val value: String): MockSqsAttribute()
    data class TextList(val value: List<String>): MockSqsAttribute()
    data class Binary(val value: ByteBuffer): MockSqsAttribute()
    data class BinaryList(val value: List<ByteBuffer>): MockSqsAttribute()
    data class Number(val value: Long): MockSqsAttribute()
    data class NumberList(val value: List<Long>): MockSqsAttribute()
}



