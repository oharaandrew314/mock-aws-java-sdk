package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.SsmUtils.assertIsCorrect
import org.assertj.core.api.Assertions.*
import io.andrewohara.awsmock.ssm.SsmUtils.set
import io.andrewohara.awsmock.ssm.SsmUtils.delete
import org.junit.After
import org.junit.Test

class GetParameterTest {

    private val client = MockAWSSimpleSystemsManagement()

    @After
    fun cleanup() {
        client.delete("foo")
    }

    @Test
    fun `get missing parameter`() {
        val exception = catchThrowableOfType(
                { client.getParameter(GetParameterRequest().withName("foo")) },
                ParameterNotFoundException::class.java
        )
        exception.assertIsCorrect()
    }

    @Test
    fun `get string`() {
        client["foo"] = "bar"

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("bar")
                        .withVersion(1)
        )
    }

    @Test
    fun `get string at version 2`() {
        client["foo"] = "bar"
        client["foo"] = "baz"

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("baz")
                        .withVersion(2)
        )
    }

    @Test
    fun `get string list`() {
        client["foo"] = listOf("bar", "baz")

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.StringList)
                        .withValue("bar,baz")
                        .withVersion(1)
        )
    }

    @Test
    fun `get string with decryption`() {
        client["foo"] = "bar"

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
                        .withWithDecryption(true)
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("bar")
                        .withVersion(1)
        )
    }

    @Test
    fun `get secure string without decryption`() {
        client["foo"] = SecureParam(value = "bar")

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("defaultKey~bar")
                        .withVersion(1)
        )
    }

    @Test
    fun `get secure string parameter with decryption`() {
        client["foo"] = SecureParam(value ="bar")

        val response = client.getParameter(
                GetParameterRequest()
                        .withName("foo")
                        .withWithDecryption(true)
        )

        assertThat(response.parameter).isEqualTo(
                Parameter()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("bar")
                        .withVersion(1)
        )
    }
}