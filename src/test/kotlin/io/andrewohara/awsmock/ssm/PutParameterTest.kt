package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.*
import io.andrewohara.awsmock.ssm.SsmUtils.assertIsCorrect
import io.andrewohara.awsmock.ssm.SsmUtils.assertIsKeyIdNotRequired
import org.assertj.core.api.Assertions.*
import io.andrewohara.awsmock.ssm.SsmUtils.get
import io.andrewohara.awsmock.ssm.SsmUtils.set
import org.junit.jupiter.api.Test

class PutParameterTest {

    private val client = MockAWSSimpleSystemsManagement()

    @Test
    fun `put string`() {
        val request = PutParameterRequest()
                .withType(ParameterType.String)
                .withName("foo")
                .withValue("bar")

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(1)
        assertThat(client["foo"]).isEqualTo("bar")
    }

    @Test
    fun `put string with key id`() {
        val request = PutParameterRequest()
                .withType(ParameterType.String)
                .withName("foo")
                .withValue("bar")
                .withKeyId("secretKey")

        val exception = catchThrowableOfType(
                { client.putParameter(request) },
                AWSSimpleSystemsManagementException::class.java
        )

        exception.assertIsKeyIdNotRequired()
    }

    @Test
    fun `put string list`() {
        val request = PutParameterRequest()
                .withType(ParameterType.StringList)
                .withName("foo")
                .withValue("bar")

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(1)
        assertThat(client["foo"]).isEqualTo("bar")
    }

    @Test
    fun `put secure string without key id`() {
        val request = PutParameterRequest()
                .withType(ParameterType.SecureString)
                .withName("foo")
                .withValue("bar")

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(1)
        assertThat(client["foo"]).isEqualTo("defaultKey~bar")
    }

    @Test
    fun `put secure string with key id`() {
        val request = PutParameterRequest()
                .withType(ParameterType.SecureString)
                .withName("foo")
                .withValue("bar")
                .withKeyId("secretKey")

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(1)
        assertThat(client["foo"]).isEqualTo("secretKey~bar")
    }

    @Test
    fun `put existing parameter without overwrite`() {
        client["foo"] = "bar"

        val request = PutParameterRequest()
                .withType(ParameterType.SecureString)
                .withName("foo")
                .withValue("baz")

        val exception = catchThrowableOfType(
                { client.putParameter(request) },
                ParameterAlreadyExistsException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `put existing parameter with overwrite`() {
        client["foo"] = "bar"

        val request = PutParameterRequest()
                .withType(ParameterType.String)
                .withName("foo")
                .withValue("baz")
                .withOverwrite(true)

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(2)
        assertThat(client["foo"]).isEqualTo("baz")
    }

    @Test
    fun `overwrite secure string with new key`() {
        client["foo"] = SecureParam("bar")

        val request = PutParameterRequest()
                .withType(ParameterType.SecureString)
                .withName("foo")
                .withValue("baz")
                .withOverwrite(true)
                .withKeyId("secretKey")

        val resp = client.putParameter(request)

        assertThat(resp.version).isEqualTo(2)
        assertThat(client["foo"]).isEqualTo("secretKey~baz")
    }

    @Test
    fun `overwrite string to secure string`() {
        client["foo"] = "bar"

        val request = PutParameterRequest()
                .withType(ParameterType.SecureString)
                .withName("foo")
                .withValue("baz")
                .withOverwrite(true)
                .withKeyId("secretKey")

        client.putParameter(request)
        assertThat(client["foo"]).isEqualTo("secretKey~baz")
    }
}