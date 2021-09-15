package io.andrewohara.awsmock.ssm.v1

import com.amazonaws.services.simplesystemsmanagement.model.*
import io.andrewohara.awsmock.ssm.MockSsmV1
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class MockSsmV1DescribeParametersTest {

    private val backend = MockSsmBackend()
    private val client = MockSsmV1(backend)

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

        val request = DescribeParametersRequest()
        val resp = client.describeParameters(request)

        resp.parameters.shouldContainExactly(
            ParameterMetadata()
                .withName("name")
                .withType(ParameterType.String)
                .withVersion(2),
            ParameterMetadata()
                .withName("cats")
                .withType(ParameterType.StringList)
                .withVersion(1),
            ParameterMetadata()
                .withName("password")
                .withType(ParameterType.SecureString)
                .withKeyId("secretKey")
                .withVersion(1),
            ParameterMetadata()
                .withName("bankAccountNumber")
                .withType(ParameterType.SecureString)
                .withKeyId("defaultKey")
                .withDescription("this must be kept super secret")
                .withVersion(1)
        )
    }

    @Test
    fun `describe parameters with max results`() {
        backend["name"] = "Andrew"
        backend["cats"] = listOf("Toggles", "Smokey", "Bandit")

        val request = DescribeParametersRequest().withMaxResults(1)

        val resp = client.describeParameters(request)

        resp.parameters.shouldContainExactly(
            ParameterMetadata()
                .withName("name")
                .withType(ParameterType.String)
                .withVersion(1)
        )
    }
}