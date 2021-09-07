package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsManagerBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretsV1DeleteSecretTest {

    private val backend = MockSecretsManagerBackend()
    private val client = MockSecretsManagerV1(backend)
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
        val secret = backend.create(name, secretString = "foo")

        client.deleteSecret(DeleteSecretRequest().withSecretId(secret.arn))

        assertThat(backend[secret.name]?.deleted).isTrue
    }

    @Test
    fun `delete secret by name`() {
        val secret = backend.create(name, secretString = "foo")

        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        assertThat(backend[secret.name]?.deleted).isTrue
    }

    @Test
    fun `delete deleted secret - no error`() {
        val secret = backend.create(name, secretString = "foo")
        backend.deleteSecret(secret.name)

        client.deleteSecret(DeleteSecretRequest().withSecretId(name))
    }
}