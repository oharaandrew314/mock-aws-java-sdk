package io.andrewohara.awsmock.samples.sqs

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.MockSqsV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CatSpaUnitTest {

    private val backend = MockSqsBackend()
    private val queue = backend.create("grooming")!!
    private val testObj = CatSpa(MockSqsV2(backend), queue.url)

    @Test
    fun `start appointment - empty backlog`() {
        testObj.startAppointment("Toggles")

        assertThat(queue.messages.map { it.body }).containsExactly("Toggles")
    }

    @Test
    fun `start appointment - with existing backlog`() {
        queue.send("Toggles")

        testObj.startAppointment("Titan")

        assertThat(queue.messages.map { it.body }).containsExactly("Toggles", "Titan")
    }
}