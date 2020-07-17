package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.CreateSecretRequest
import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.util.*

class DeleteSecretTest {

    private val client = MockAWSSecretsManager()
    private val name = UUID.randomUUID().toString()

    @Test
    fun `delete missing secret`() {
        val exception = catchThrowableOfType(
                { client.deleteSecret(DeleteSecretRequest().withSecretId(name)) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `delete secret by arn`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("foo"))

        client.deleteSecret(DeleteSecretRequest().withSecretId(created.arn))

        assertThatThrownBy { client.getSecretValue(GetSecretValueRequest().withSecretId(UUID.randomUUID().toString())) }
                .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `delete secret by name`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("foo"))

        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        assertThatThrownBy { client.getSecretValue(GetSecretValueRequest().withSecretId(UUID.randomUUID().toString())) }
                .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `delete deleted secret`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("foo"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        client.deleteSecret(DeleteSecretRequest().withSecretId(name))
    }
}