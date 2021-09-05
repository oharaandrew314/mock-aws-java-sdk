package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.SsmUtils.set
import io.andrewohara.awsmock.ssm.SsmUtils.delete
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class GetParametersTest {

    private val client = MockAWSSimpleSystemsManagement()

    @Test
    fun `get missing parameters`() {
        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        assertThat(resp.invalidParameters).containsExactlyInAnyOrder("name", "password")
        assertThat(resp.parameters).isEmpty()
    }

    @Test
    fun `get parameters`() {
        client["name"] = "Andrew"
        client["password"] = "hunter2"

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        assertThat(resp.invalidParameters).isEmpty()
        assertThat(resp.parameters).containsExactlyInAnyOrder(
                Parameter().withName("name").withValue("Andrew").withType(ParameterType.String).withVersion(1),
                Parameter().withName("password").withValue("hunter2").withType(ParameterType.String).withVersion(1)
        )
    }

    @Test
    fun `get encrypted parameters`() {
        client["name"] = SecureParam("Andrew")
        client["password"] = SecureParam("hunter2")

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password"))

        assertThat(resp.invalidParameters).isEmpty()
        assertThat(resp.parameters).containsExactlyInAnyOrder(
                Parameter().withName("name").withValue("defaultKey~Andrew").withType(ParameterType.SecureString).withVersion(1),
                Parameter().withName("password").withValue("defaultKey~hunter2").withType(ParameterType.SecureString).withVersion(1)
        )
    }

    @Test
    fun `get decrypted parameters`() {
        client["name"] = SecureParam("Andrew")
        client["password"] = SecureParam("hunter2")

        val resp = client.getParameters(GetParametersRequest().withNames("name", "password").withWithDecryption(true))

        assertThat(resp.invalidParameters).isEmpty()
        assertThat(resp.parameters).containsExactlyInAnyOrder(
                Parameter().withName("name").withValue("Andrew").withType(ParameterType.SecureString).withVersion(1),
                Parameter().withName("password").withValue("hunter2").withType(ParameterType.SecureString).withVersion(1)
        )
    }
}