package io.andrewohara.awsmock.sqs.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.UnsupportedOperationException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class MockSqsBackendTest {

    private var time = Instant.ofEpochSecond(9001)
    private val backend = MockSqsBackend(clock = object: Clock() {
        override fun getZone() = throw UnsupportedOperationException()
        override fun withZone(zone: ZoneId) = throw UnsupportedOperationException()
        override fun instant() = time
    })

    private val queue = backend.create("foo")!!

    @Test
    fun `can't receive message before delay expires`() {
        queue.send("foo", delay = Duration.ofSeconds(30))

        assertThat(queue.receive()).isEmpty()
    }

    @Test
    fun `receive message after delay expires`() {
        queue.send("foo", delay = Duration.ofSeconds(30))
        time += Duration.ofMinutes(1)

        assertThat(queue.receive()).hasSize(1)
    }

    @Test
    fun `can't receive message again before visibility timeout expires`() {
        queue.send("foo")
        queue.receive(visibilityTimeout = Duration.ofSeconds(5))

        assertThat(queue.receive()).isEmpty()
    }

    @Test
    fun `receive message again after visibility timeout expires`() {
        queue.send("foo")
        queue.receive(visibilityTimeout = Duration.ofSeconds(5))
        time += Duration.ofSeconds(10)

        assertThat(queue.receive()).hasSize(1)
    }
}