package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.DeleteParametersRequest
import com.amazonaws.services.simplesystemsmanagement.model.DeleteParametersResult
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MockSsmV1DeleteParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `delete missing parameters`() {
        val resp = client.deleteParameters(DeleteParametersRequest().withNames("name"))

        resp shouldBe DeleteParametersResult()
            .withDeletedParameters(emptyList())
            .withInvalidParameters("name")
    }

    @Test
    fun `delete parameters`() {
        backend["name"] = "Andrew"
        backend["cat"] = "Toggles"

        val resp = client.deleteParameters(DeleteParametersRequest().withNames("name", "cat", "dog"))

        resp shouldBe DeleteParametersResult()
            .withDeletedParameters("name", "cat")
            .withInvalidParameters("dog")

        backend.parameters().shouldBeEmpty()
    }
}