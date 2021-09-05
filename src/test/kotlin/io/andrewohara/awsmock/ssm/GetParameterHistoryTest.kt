package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.GetParameterHistoryRequest
import com.amazonaws.services.simplesystemsmanagement.model.ParameterHistory
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType
import io.andrewohara.awsmock.ssm.SsmUtils.assertIsCorrect
import io.andrewohara.awsmock.ssm.SsmUtils.set
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class GetParameterHistoryTest {

    private val client = MockAWSSimpleSystemsManagement()

    @Test
    fun `get history for missing string`() {
        val exception = catchThrowableOfType(
                { client.getParameterHistory(GetParameterHistoryRequest().withName("foo")) },
                ParameterNotFoundException::class.java
        )
        exception.assertIsCorrect()
    }

    @Test
    fun `get history for string`() {
        client["foo"] = "bar"
        client["foo"] = Param("baz", description = "stuff")

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo"))

        assertThat(result.parameters).containsExactly(
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("bar")
                        .withVersion(1),
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("baz")
                        .withDescription("stuff")
                        .withVersion(2)
        )
    }

    @Test
    fun `get encrypted history for secure string`() {
        client["foo"] = SecureParam("bar")
        client["foo"] = SecureParam("baz", keyId = "secretKey", description = "secret stuff")

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo"))

        assertThat(result.parameters).containsExactly(
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("defaultKey~bar")
                        .withKeyId("defaultKey")
                        .withVersion(1),
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("secretKey~baz")
                        .withKeyId("secretKey")
                        .withDescription("secret stuff")
                        .withVersion(2)
        )
    }

    @Test
    fun `get decrypted history for secure string`() {
        client["foo"] = SecureParam("bar")
        client["foo"] = SecureParam("baz", keyId = "secretKey", description = "secret stuff")

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo").withWithDecryption(true))

        assertThat(result.parameters).containsExactly(
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("bar")
                        .withKeyId("defaultKey")
                        .withVersion(1),
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("baz")
                        .withKeyId("secretKey")
                        .withDescription("secret stuff")
                        .withVersion(2)
        )
    }

    @Test
    fun `get history for string that changed to secure string`() {
        client["foo"] = Param("bar")
        client["foo"] = SecureParam("baz")

        val result = client.getParameterHistory(GetParameterHistoryRequest().withName("foo").withWithDecryption(true))

        assertThat(result.parameters).containsExactly(
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.String)
                        .withValue("bar")
                        .withVersion(1),
                ParameterHistory()
                        .withName("foo")
                        .withType(ParameterType.SecureString)
                        .withValue("baz")
                        .withKeyId("defaultKey")
                        .withVersion(2)
        )
    }
}