package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.*
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MockSsmV1PutParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `put string`() {
        val request = PutParameterRequest()
            .withType(ParameterType.String)
            .withName("foo")
            .withValue("bar")

        val resp = client.putParameter(request)

        resp shouldBe PutParameterResult()
            .withVersion(1)

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
        val request = PutParameterRequest()
            .withType(ParameterType.String)
            .withName("foo")
            .withValue("bar")
            .withKeyId("secretKey")

        val exception = shouldThrow<AWSSimpleSystemsManagementException> {
            client.putParameter(request)
        }

        exception.errorCode shouldBe "ValidationException"
        exception.errorMessage shouldBe "KeyId is required for SecureString type parameter only."
    }

    @Test
    fun `put string list`() {
        val request = PutParameterRequest()
            .withType(ParameterType.StringList)
            .withName("foo")
            .withValue("bar,baz")

        val resp = client.putParameter(request)
        resp shouldBe PutParameterResult().withVersion(1)

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
        val request = PutParameterRequest()
            .withType(ParameterType.SecureString)
            .withName("foo")
            .withValue("bar")

        val resp = client.putParameter(request)
        resp shouldBe PutParameterResult().withVersion(1)

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
        val request = PutParameterRequest()
            .withType(ParameterType.SecureString)
            .withName("foo")
            .withValue("bar")
            .withKeyId("secretKey")

        val resp = client.putParameter(request)
        resp shouldBe PutParameterResult().withVersion(1)

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

        val request = PutParameterRequest()
            .withType(ParameterType.SecureString)
            .withName("foo")
            .withValue("baz")

        shouldThrow<ParameterAlreadyExistsException> {
            client.putParameter(request)
        }
    }

    @Test
    fun `put existing parameter with overwrite`() {
        backend["foo"] = "bar"

        val request = PutParameterRequest()
            .withType(ParameterType.String)
            .withName("foo")
            .withValue("baz")
            .withOverwrite(true)

        val resp = client.putParameter(request)
        resp shouldBe PutParameterResult().withVersion(2)

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