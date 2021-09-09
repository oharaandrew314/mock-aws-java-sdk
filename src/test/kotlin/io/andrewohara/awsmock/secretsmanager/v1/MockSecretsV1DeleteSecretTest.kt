package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest
import com.amazonaws.services.secretsmanager.model.DeleteSecretResult
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretsV1DeleteSecretTest {

    private val backend = MockSecretsBackend()
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
    fun `delete secret`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.deleteSecret(DeleteSecretRequest().withSecretId(secret.arn))

        assertThat(resp).isEqualTo(
            DeleteSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
        )

        assertThat(backend[secret.name]?.deleted).isTrue
    }
}