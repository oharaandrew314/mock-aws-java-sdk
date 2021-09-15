package io.andrewohara.awsmock.ssm.v2

import io.andrewohara.awsmock.ssm.MockSsmV2
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ssm.model.DescribeParametersResponse
import software.amazon.awssdk.services.ssm.model.ParameterMetadata
import software.amazon.awssdk.services.ssm.model.ParameterType

class MockSsmV2DescribeParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV2(backend)

    @Test
    fun `describe parameters with no filters`() {
        backend["name"] = "todo"
        backend["name"] = "Andrew"
        backend["cats"] = listOf("Toggles", "Smokey", "Bandit")
        backend.add("password", type = MockSsmParameter.Type.Secure, value = "hunter2", keyId = "secretKey")
        backend.add(
            "bankAccountNumber",
            type = MockSsmParameter.Type.Secure,
            value = "1337",
            description = "this must be kept super secret"
        )

        client.describeParameters() shouldBe DescribeParametersResponse.builder()
            .parameters(
                ParameterMetadata.builder()
                    .name("name")
                    .type(ParameterType.STRING)
                    .version(2)
                    .build(),
                ParameterMetadata.builder()
                    .name("cats")
                    .type(ParameterType.STRING_LIST)
                    .version(1)
                    .build(),
                ParameterMetadata.builder()
                    .name("password")
                    .type(ParameterType.SECURE_STRING)
                    .keyId("secretKey")
                    .version(1)
                    .build(),
                ParameterMetadata.builder()
                    .name("bankAccountNumber")
                    .type(ParameterType.SECURE_STRING)
                    .keyId("defaultKey")
                    .description("this must be kept super secret")
                    .version(1)
                    .build()
            ).build()
    }

    @Test
    fun `describe parameters with max results`() {
        backend["name"] = "Andrew"
        backend["cats"] = listOf("Toggles", "Smokey", "Bandit")

        client.describeParameters {
            it.maxResults(1)
        } shouldBe DescribeParametersResponse.builder()
            .parameters(
                ParameterMetadata.builder()
                    .name("name")
                    .type(ParameterType.STRING)
                    .version(1)
                    .build(),
            )
            .build()
    }
}