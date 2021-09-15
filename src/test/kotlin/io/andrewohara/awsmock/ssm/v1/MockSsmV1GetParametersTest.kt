package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersResult
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MockSsmV1GetParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `get missing parameters`() {
        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        resp shouldBe GetParametersResult()
            .withParameters(emptyList())
            .withInvalidParameters("name", "password")
    }

    @Test
    fun `get parameters`() {
        backend["name"] = "Andrew"
        backend["password"] = "hunter2"

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        resp shouldBe GetParametersResult()
            .withParameters(
                Parameter()
                    .withName("name")
                    .withValue("Andrew")
                    .withType(ParameterType.String)
                    .withVersion(1),
                Parameter()
                    .withName("password")
                    .withValue("hunter2")
                    .withType(ParameterType.String)
                    .withVersion(1)
            )
            .withInvalidParameters(emptyList())
    }

    @Test
    fun `get encrypted parameters`() {
        backend.add(name = "name", type = MockSsmParameter.Type.Secure, value ="Andrew")
        backend.add(name = "password", type = MockSsmParameter.Type.Secure, value = "hunter2")

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        resp shouldBe GetParametersResult()
            .withParameters(
                Parameter()
                    .withName("name")
                    .withValue("defaultKey~Andrew")
                    .withType(ParameterType.SecureString)
                    .withVersion(1),
                Parameter()
                    .withName("password")
                    .withValue("defaultKey~hunter2")
                    .withType(ParameterType.SecureString)
                    .withVersion(1)
            )
            .withInvalidParameters(emptyList())
    }

    @Test
    fun `get decrypted parameters`() {
        backend.add(name = "name", type = MockSsmParameter.Type.Secure, value = "Andrew")
        backend.add(name = "password", type = MockSsmParameter.Type.Secure, value = "hunter2")

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password").withWithDecryption(true))

        resp shouldBe GetParametersResult()
            .withParameters(
                Parameter()
                    .withName("name")
                    .withValue("Andrew")
                    .withType(ParameterType.SecureString)
                    .withVersion(1),
                Parameter()
                    .withName("password")
                    .withValue("hunter2")
                    .withType(ParameterType.SecureString)
                    .withVersion(1)
            )
    }
}