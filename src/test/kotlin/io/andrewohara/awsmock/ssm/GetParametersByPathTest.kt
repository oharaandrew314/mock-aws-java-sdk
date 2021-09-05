package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.SsmUtils.set
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class GetParametersByPathTest {

    private val client = MockAWSSimpleSystemsManagement()

    init {
        client["/cats/host"] = "https://cats.meow"
        client["/cats/apiKey"] = SecureParam("hunter2")

        client["/doggos/host"] = "https://doggos.woof"
        client["/doggos/apiKey"] = SecureParam("woof-and-attac")
    }

    @Test
    fun `get encrypted cats parameters`() {
        val request = GetParametersByPathRequest().withPath("/cats")

        val resp = client.getParametersByPath(request)

        assertThat(resp.parameters).containsExactlyInAnyOrder(
                Parameter().withName("/cats/host").withType(ParameterType.String).withValue("https://cats.meow").withVersion(1),
                Parameter().withName("/cats/apiKey").withType(ParameterType.SecureString).withValue("defaultKey~hunter2").withVersion(1)
        )
    }

    @Test
    fun `get decrypted doggo parameters`() {
        val request = GetParametersByPathRequest().withPath("/doggos").withWithDecryption(true)

        val resp = client.getParametersByPath(request)

        assertThat(resp.parameters).containsExactlyInAnyOrder(
                Parameter().withName("/doggos/host").withType(ParameterType.String).withValue("https://doggos.woof").withVersion(1),
                Parameter().withName("/doggos/apiKey").withType(ParameterType.SecureString).withValue("woof-and-attac").withVersion(1)
        )
    }

    @Test
    fun `get missing lizard parameters`() {
        val request = GetParametersByPathRequest().withPath("/lizards")

        val resp = client.getParametersByPath(request)

        assertThat(resp.parameters).isEmpty()
    }

    @Test
    fun `get cats parameters with max results`() {
        val request = GetParametersByPathRequest().withPath("/cats").withMaxResults(1)

        val resp = client.getParametersByPath(request)

        assertThat(resp.parameters).hasSize(1)
    }
}