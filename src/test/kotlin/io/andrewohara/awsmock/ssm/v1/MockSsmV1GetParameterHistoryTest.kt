package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.GetParameterHistoryRequest
import com.amazonaws.services.simplesystemsmanagement.model.ParameterHistory
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class MockSsmV1GetParameterHistoryTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `get history for missing string`() {
        shouldThrow<ParameterNotFoundException> {
            client.getParameterHistory(GetParameterHistoryRequest().withName("foo"))
        }
    }

    @Test
    fun `get history for string`() {
        backend["foo"] = "bar"
        backend.add("foo", type = MockSsmParameter.Type.String, value = "baz", description = "stuff", overwrite = true)

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo"))

        result.parameters.shouldContainExactly(
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.String)
                .withValue("bar")
                .withVersion(1),
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.String)
                .withValue("baz")
                .withDescription("stuff")
                .withVersion(2)
        )
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

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo"))

        result.parameters.shouldContainExactly(
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.SecureString)
                .withValue("defaultKey~bar")
                .withKeyId("defaultKey")
                .withVersion(1),
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.SecureString)
                .withValue("secretKey~baz")
                .withKeyId("secretKey")
                .withDescription("secret stuff")
                .withVersion(2)
        )
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

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo").withWithDecryption(true))

        result.parameters.shouldContainExactly(
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.SecureString)
                .withValue("bar")
                .withKeyId("defaultKey")
                .withVersion(1),
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.SecureString)
                .withValue("baz")
                .withKeyId("secretKey")
                .withDescription("secret stuff")
                .withVersion(2)
        )
    }

    @Test
    fun `get history for string that changed to secure string`() {
        val param = backend.set("foo", "bar")
        param.add(MockSsmParameter.Type.Secure, "baz")

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo").withWithDecryption(true))

        result.parameters.shouldContainExactly(
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.String)
                .withValue("bar")
                .withVersion(1),
            ParameterHistory()
                .withName("foo")
                .withType(ParameterType.SecureString)
                .withValue("baz")
                .withKeyId("defaultKey")
                .withVersion(2)
        )
    }
}