package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse
import software.amazon.awssdk.services.ssm.model.Parameter
import software.amazon.awssdk.services.ssm.model.ParameterType

class MockSsmV2GetParametersByPathTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    init {
        backend["/cats/host"] = "https://cats.meow"
        backend.secure("/cats/apiKey", "hunter2")

        backend["/doggos/host"] = "https://doggos.woof"
        backend.secure("/doggos/apiKey", "woof-and-attac")
    }

    @Test
    fun `get encrypted cats parameters`() {
        client.getParametersByPath {
            it.path("/cats")
        } shouldBe GetParametersByPathResponse.builder()
            .parameters(
                Parameter.builder()
                    .name("/cats/host")
                    .type(ParameterType.STRING)
                    .value("https://cats.meow")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/cats/host")
                    .build(),
                Parameter.builder()
                    .name("/cats/apiKey")
                    .type(ParameterType.SECURE_STRING)
                    .value("defaultKey~hunter2")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/cats/apiKey")
                    .build()
            ).build()
    }

    @Test
    fun `get decrypted doggo parameters`() {
        client.getParametersByPath {
            it.path("/doggos")
            it.withDecryption(true)
        } shouldBe GetParametersByPathResponse.builder()
            .parameters(
                Parameter.builder()
                    .name("/doggos/host")
                    .type(ParameterType.STRING)
                    .value("https://doggos.woof")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/doggos/host")
                    .build(),
                Parameter.builder()
                    .name("/doggos/apiKey")
                    .type(ParameterType.SECURE_STRING)
                    .value("woof-and-attac")
                    .version(1)
                    .arn("arn:aws:ssm-mock:ca-central-1:111222333444:parameter/doggos/apiKey")
                    .build()
            ).build()
    }

    @Test
    fun `get missing lizard parameters`() {
        client.getParametersByPath {
            it.path("/lizards")
        } shouldBe GetParametersByPathResponse.builder()
            .parameters(emptyList())
            .build()
    }

    @Test
    fun `get cats parameters with max results`() {
        val resp = client.getParametersByPath {
            it.path("/cats")
            it.maxResults(1)
        }

        resp.parameters().shouldHaveSize(1)
    }
}