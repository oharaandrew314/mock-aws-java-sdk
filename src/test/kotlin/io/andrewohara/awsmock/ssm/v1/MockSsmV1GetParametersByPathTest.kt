package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class MockSsmV1GetParametersByPathTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    init {
        backend["/cats/host"] = "https://cats.meow"
        backend.secure("/cats/apiKey", "hunter2")

        backend["/doggos/host"] = "https://doggos.woof"
        backend.secure("/doggos/apiKey", "woof-and-attac")
    }

    @Test
    fun `get encrypted cats parameters`() {
        val request = GetParametersByPathRequest().withPath("/cats")

        val resp = client.getParametersByPath(request)

        resp.parameters.shouldContainExactly(
            Parameter()
                .withName("/cats/host")
                .withType(ParameterType.String)
                .withValue("https://cats.meow")
                .withVersion(1),
            Parameter()
                .withName("/cats/apiKey")
                .withType(ParameterType.SecureString)
                .withValue("defaultKey~hunter2")
                .withVersion(1)
        )
    }

    @Test
    fun `get decrypted doggo parameters`() {
        val request = GetParametersByPathRequest().withPath("/doggos").withWithDecryption(true)

        val resp = client.getParametersByPath(request)

        resp.parameters.shouldContainExactly(
            Parameter()
                .withName("/doggos/host")
                .withType(ParameterType.String)
                .withValue("https://doggos.woof")
                .withVersion(1),
            Parameter()
                .withName("/doggos/apiKey")
                .withType(ParameterType.SecureString)
                .withValue("woof-and-attac")
                .withVersion(1)
        )
    }

    @Test
    fun `get missing lizard parameters`() {
        val request = GetParametersByPathRequest().withPath("/lizards")

        val resp = client.getParametersByPath(request)

        resp.parameters.shouldBeEmpty()
    }

    @Test
    fun `get cats parameters with max results`() {
        val request = GetParametersByPathRequest().withPath("/cats").withMaxResults(1)

        val resp = client.getParametersByPath(request)

        resp.parameters.shouldHaveSize(1)
    }
}