package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.*
import org.assertj.core.api.Assertions.*
import io.andrewohara.awsmock.ssm.SsmUtils.set
import io.andrewohara.awsmock.ssm.SsmUtils.delete
import org.junit.After
import org.junit.Test

class DescribeParametersTest {

    private val client = MockAWSSimpleSystemsManagement()

    @After
    fun cleanup() {
        client.delete("name")
        client.delete("cats")
        client.delete("password")
        client.delete("bankAccountNumber")
    }

    @Test
    fun `describe parameters with no filters`() {
        client["name"] = "todo"
        client["name"] = "Andrew"
        client["cats"] = listOf("Toggles", "Smokey", "Bandit")
        client["password"] = SecureParam(value = "hunter2", keyId = "secretKey")
        client["bankAccountNumber"] = SecureParam(value = "1337", description = "this must be kept super secret")

        val request = DescribeParametersRequest()

        val resp = client.describeParameters(request)

        assertThat(resp.parameters).contains(
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
        client["name"] = "Andrew"
        client["cats"] = listOf("Toggles", "Smokey", "Bandit")

        val request = DescribeParametersRequest().withMaxResults(1)

        val resp = client.describeParameters(request)

        assertThat(resp.parameters).hasSize(1)
    }
}