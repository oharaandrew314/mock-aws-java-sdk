package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.GetParametersResponse
import software.amazon.awssdk.services.ssm.model.Parameter
import software.amazon.awssdk.services.ssm.model.ParameterType

class MockSsmV2GetParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `get missing parameters`() {
        client.getParameters {
            it.names("name", "password")
        } shouldBe GetParametersResponse.builder()
            .parameters(emptyList())
            .invalidParameters("name", "password")
            .build()
    }

    @Test
    fun `get parameters`() {
        backend["name"] = "Andrew"
        backend["password"] = "hunter2"

        client.getParameters {
            it.names("name", "password")
        } shouldBe GetParametersResponse.builder()
            .parameters(
                Parameter.builder()
                    .name("name")
                    .value("Andrew")
                    .type(ParameterType.STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/name")
                    .build(),
                Parameter.builder()
                    .name("password")
                    .value("hunter2")
                    .type(ParameterType.STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/password")
                    .build()
            )
            .invalidParameters(emptyList())
            .build()
    }

    @Test
    fun `get encrypted parameters`() {
        backend.add(name = "name", type = MockSsmParameter.Type.Secure, value ="Andrew")
        backend.add(name = "password", type = MockSsmParameter.Type.Secure, value = "hunter2")

        client.getParameters {
            it.names("name", "password")
        } shouldBe GetParametersResponse.builder()
            .parameters(
                Parameter.builder()
                    .name("name")
                    .value("defaultKey~Andrew")
                    .type(ParameterType.SECURE_STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/name")
                    .build(),
                Parameter.builder()
                    .name("password")
                    .value("defaultKey~hunter2")
                    .type(ParameterType.SECURE_STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/password")
                    .build()
            )
            .invalidParameters(emptyList())
            .build()
    }

    @Test
    fun `get decrypted parameters`() {
        backend.add(name = "name", type = MockSsmParameter.Type.Secure, value ="Andrew")
        backend.add(name = "password", type = MockSsmParameter.Type.Secure, value = "hunter2")

        client.getParameters {
            it.names("name", "password")
            it.withDecryption(true)
        } shouldBe GetParametersResponse.builder()
            .parameters(
                Parameter.builder()
                    .name("name")
                    .value("Andrew")
                    .type(ParameterType.SECURE_STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/name")
                    .build(),
                Parameter.builder()
                    .name("password")
                    .value("hunter2")
                    .type(ParameterType.SECURE_STRING)
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/password")
                    .build()
            )
            .invalidParameters(emptyList())
            .build()
    }
}