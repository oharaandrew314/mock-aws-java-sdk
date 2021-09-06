package io.andrewohara.awsmock.sqs.backend

import java.time.Clock
import java.time.Duration
import java.util.*

class MockSqsQueue internal constructor(
    private val clock: Clock,
    val name: String,
    val url: String,
    val attributes: MutableMap<String, String>
) {
    companion object {
        private val validVisibilityTimeouts = Duration.ZERO..Duration.ofHours(12)
    }

    val messages = mutableListOf<MockSqsMessage>()
    private val receipts = mutableListOf<MockSqsReceipt>()

    fun defaultVisibilityTimeout(): Duration = attributes["VisibilityTimeout"]
        ?.let { Duration.ofSeconds(it.toLong()) }
        ?: Duration.ofSeconds(30)
    fun defaultDelay(): Duration = attributes["DelaySeconds"]
        ?.let { Duration.ofSeconds(it.toLong()) }
        ?: Duration.ZERO

    fun send(body: String, delay: Duration? = null) = MockSqsMessage(
        id = UUID.randomUUID().toString(),
        body = body,
        visibleAt = clock.instant() + (delay ?: defaultDelay())
    ).also { messages += it }

    fun receive(limit: Int = Int.MAX_VALUE, visibilityTimeout: Duration? = null): List<MockSqsReceipt> {
        val time = clock.instant()
        return messages
            .asSequence()
            .filter { time >= it.visibleAt }
            .take(limit)
            .map { message ->
                message.visibleAt = time + (visibilityTimeout ?: defaultVisibilityTimeout())
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

    fun updateVisibilityTimeout(receiptHandle: String, timeout: Duration): UpdateVisibilityResult {
        val receipt = receipts.find { it.receiptHandle == receiptHandle } ?: return UpdateVisibilityResult.NotFound
        if (timeout !in validVisibilityTimeouts) return UpdateVisibilityResult.InvalidTimeout

        receipt.message.visibleAt = clock.instant() + timeout

        return UpdateVisibilityResult.Updated
    }

    enum class UpdateVisibilityResult { Updated, NotFound, InvalidTimeout }
}