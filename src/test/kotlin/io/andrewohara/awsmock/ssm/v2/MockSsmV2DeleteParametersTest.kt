package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.DeleteParametersResponse

class MockSsmV2DeleteParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `delete missing parameters`() {
        client.deleteParameters {
            it.names("name")
        } shouldBe DeleteParametersResponse.builder()
            .deletedParameters(emptyList())
            .invalidParameters("name")
            .build()
    }

    @Test
    fun `delete parameters`() {
        backend["name"] = "Andrew"
        backend["cat"] = "Toggles"

        client.deleteParameters {
            it.names("name", "cat", "dog")
        } shouldBe DeleteParametersResponse.builder()
            .deletedParameters("name", "cat")
            .invalidParameters("dog")
            .build()

        backend.parameters().shouldBeEmpty()
    }
}