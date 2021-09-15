package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.*
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MockSsmV1GetParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

    @Test
    fun `get missing parameter`() {
        shouldThrow<ParameterNotFoundException> {
            client.getParameter(GetParameterRequest().withName("foo"))
        }
    }

    @Test
    fun `get string`() {
        backend["foo"] = "bar"

        val request = GetParameterRequest().withName("foo")
        val response = client.getParameter(request)

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.String)
                    .withValue("bar")
                    .withVersion(1)
            )
    }

    @Test
    fun `get string at version 2`() {
        backend["foo"] = "bar"
        backend["foo"] = "baz"

        val response = client.getParameter(
            GetParameterRequest()
                .withName("foo")
        )

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.String)
                    .withValue("baz")
                    .withVersion(2)
            )
    }

    @Test
    fun `get string list`() {
        backend["foo"] = listOf("bar", "baz")

        val response = client.getParameter(
            GetParameterRequest()
                .withName("foo")
        )

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.StringList)
                    .withValue("bar,baz")
                    .withVersion(1)
            )
    }

    @Test
    fun `get string with decryption`() {
        backend["foo"] = "bar"

        val response = client.getParameter(
            GetParameterRequest()
                .withName("foo")
                .withWithDecryption(true)
        )

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.String)
                    .withValue("bar")
                    .withVersion(1)
            )
    }

    @Test
    fun `get secure string without decryption`() {
        backend.secure("foo", "bar")

        val response = client.getParameter(
            GetParameterRequest()
                .withName("foo")
        )

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.SecureString)
                    .withValue("defaultKey~bar")
                    .withVersion(1)
            )
    }

    @Test
    fun `get secure string parameter with decryption`() {
        backend.secure("foo", "bar")

        val response = client.getParameter(
            GetParameterRequest()
                .withName("foo")
                .withWithDecryption(true)
        )

        response shouldBe GetParameterResult()
            .withParameter(
                Parameter()
                    .withName("foo")
                    .withType(ParameterType.SecureString)
                    .withValue("bar")
                    .withVersion(1)
            )
    }
}