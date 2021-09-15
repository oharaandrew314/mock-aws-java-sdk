package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.ParameterAlreadyExistsException
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterResponse
import software.amazon.awssdk.services.ssm.model.SsmException

class MockSsmV2PutParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `put string`() {
        client.putParameter {
            it.name("foo")
            it.type(ParameterType.STRING)
            it.value("bar")
        } shouldBe PutParameterResponse.builder()
            .version(1)
            .build()

        backend["foo"]?.latest() shouldBe MockSsmParameter.Value(
            type = MockSsmParameter.Type.String,
            description = null,
            keyId = null,
            value = "bar",
            version = 1
        )
    }

    @Test
    fun `put string with key_id`() {
        val exception = shouldThrow<SsmException> {
            client.putParameter {
                it.type(ParameterType.STRING)
                it.name("foo")
                it.value("bar")
                it.keyId("secretKey")
            }
        }

        exception.awsErrorDetails().errorCode() shouldBe "ValidationException"
        exception.awsErrorDetails().errorMessage() shouldBe "KeyId is required for SecureString type parameter only."
    }

    @Test
    fun `put string list`() {
        client.putParameter {
            it.type(ParameterType.STRING_LIST)
            it.name("foo")
            it.value("bar,baz")
        } shouldBe PutParameterResponse.builder()
            .version(1)
            .build()

        backend["foo"]?.latest() shouldBe MockSsmParameter.Value(
            type = MockSsmParameter.Type.StringList,
            value = "bar,baz",
            description = null,
            keyId = null,
            version = 1
        )
    }

    @Test
    fun `put secure string without key id`() {
        client.putParameter {
            it.type(ParameterType.SECURE_STRING)
            it.name("foo")
            it.value("bar")
        } shouldBe PutParameterResponse.builder()
            .version(1)
            .build()

        backend["foo"]?.latest() shouldBe MockSsmParameter.Value(
            type = MockSsmParameter.Type.Secure,
            description = null,
            keyId = "defaultKey",
            version = 1,
            value = "bar"
        )
    }

    @Test
    fun `put secure string with key id`() {
        client.putParameter {
            it.type(ParameterType.SECURE_STRING)
            it.name("foo")
            it.value("bar")
            it.keyId("secretKey")
        } shouldBe PutParameterResponse.builder()
            .version(1)
            .build()

        backend["foo"]?.latest() shouldBe MockSsmParameter.Value(
            type = MockSsmParameter.Type.Secure,
            description = null,
            keyId = "secretKey",
            version = 1,
            value = "bar"
        )
    }

    @Test
    fun `put existing parameter without overwrite`() {
        backend["foo"] = "bar"

        shouldThrow<ParameterAlreadyExistsException> {
            client.putParameter {
                it.type(ParameterType.SECURE_STRING)
                it.name("foo")
                it.value("baz")
            }
        }
    }

    @Test
    fun `put existing parameter with overwrite`() {
        backend["foo"] = "bar"

        client.putParameter {
            it.type(ParameterType.STRING)
            it.name("foo")
            it.value("baz")
            it.overwrite(true)
        } shouldBe PutParameterResponse.builder()
            .version(2)
            .build()

        backend["foo"]?.history().shouldContainExactly(
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.String,
                description = null,
                keyId = null,
                version = 1,
                value = "bar"
            ),
            MockSsmParameter.Value(
                type = MockSsmParameter.Type.String,
                description = null,
                keyId = null,
                version = 2,
                value = "baz"
            )
        )
    }
}