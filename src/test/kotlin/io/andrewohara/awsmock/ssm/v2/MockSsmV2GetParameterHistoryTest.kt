package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.GetParameterHistoryResponse
import software.amazon.awssdk.services.ssm.model.ParameterHistory
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.ParameterType

class MockSsmV2GetParameterHistoryTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `get history for missing string`() {
        shouldThrow<ParameterNotFoundException> {
            client.getParameterHistory {
                it.name("foo")
            }
        }
    }

    @Test
    fun `get history for string`() {
        backend["foo"] = "bar"
        backend.add("foo", type = MockSsmParameter.Type.String, value = "baz", description = "stuff", overwrite = true)

        client.getParameterHistory {
            it.name("foo")
        } shouldBe GetParameterHistoryResponse.builder()
            .parameters(
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.STRING)
                    .value("bar")
                    .version(1)
                    .build(),
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.STRING)
                    .value("baz")
                    .description("stuff")
                    .version(2)
                    .build()
            ).build()
    }

    @Test
    fun `get encrypted history for secure string`() {
        backend.add(name = "foo", type = MockSsmParameter.Type.Secure, value = "bar")
        backend.add(
            name = "foo",
            type = MockSsmParameter.Type.Secure,
            value = "baz",
            keyId = "secretKey",
            description = "secret stuff",
            overwrite = true
        )

        client.getParameterHistory {
            it.name("foo")
        } shouldBe GetParameterHistoryResponse.builder()
            .parameters(
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("defaultKey~bar")
                    .keyId("defaultKey")
                    .version(1)
                    .build(),
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("secretKey~baz")
                    .keyId("secretKey")
                    .description("secret stuff")
                    .version(2)
                    .build()
            )
            .build()
    }

    @Test
    fun `get decrypted history for secure string`() {
        backend.add(name = "foo", type = MockSsmParameter.Type.Secure, value = "bar")
        backend.add(
            name = "foo",
            type = MockSsmParameter.Type.Secure,
            value = "baz",
            keyId = "secretKey",
            description = "secret stuff",
            overwrite = true
        )

        client.getParameterHistory {
            it.name("foo")
            it.withDecryption(true)
        } shouldBe GetParameterHistoryResponse.builder()
            .parameters(
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("bar")
                    .keyId("defaultKey")
                    .version(1)
                    .build(),
                ParameterHistory.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("baz")
                    .keyId("secretKey")
                    .description("secret stuff")
                    .version(2)
                    .build()
            ).build()
    }
}