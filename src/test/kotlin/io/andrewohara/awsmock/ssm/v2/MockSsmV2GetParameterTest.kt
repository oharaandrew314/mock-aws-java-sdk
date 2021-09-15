package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.GetParameterResponse
import software.amazon.awssdk.services.ssm.model.Parameter
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.ParameterType

class MockSsmV2GetParameterTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `get missing parameter`() {
        shouldThrow<ParameterNotFoundException> {
            client.getParameter {
                it.name("foo")
            }
        }
    }

    @Test
    fun `get string`() {
        backend["foo"] = "bar"

        client.getParameter {
            it.name("foo")
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.STRING)
                    .value("bar")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }

    @Test
    fun `get string at version 2`() {
        backend["foo"] = "bar"
        backend["foo"] = "baz"

        client.getParameter {
            it.name("foo")
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.STRING)
                    .value("baz")
                    .version(2)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }

    @Test
    fun `get string list`() {
        backend["foo"] = listOf("bar", "baz")

        client.getParameter {
            it.name("foo")
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.STRING_LIST)
                    .value("bar,baz")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }

    @Test
    fun `get string with decryption`() {
        backend["foo"] = "bar"

        client.getParameter {
            it.name("foo")
            it.withDecryption(true)
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.STRING)
                    .value("bar")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }

    @Test
    fun `get secure string without decryption`() {
        backend.secure("foo", "bar")

        client.getParameter {
            it.name("foo")
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("defaultKey~bar")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }

    @Test
    fun `get secure string with decryption`() {
        backend.secure("foo", "bar")

        client.getParameter {
            it.name("foo")
            it.withDecryption(true)
        } shouldBe GetParameterResponse.builder()
            .parameter(
                Parameter.builder()
                    .name("foo")
                    .type(ParameterType.SECURE_STRING)
                    .value("bar")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/foo")
                    .build()
            )
            .build()
    }
}