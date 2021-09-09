package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.cannotUpdateDeletedSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class MockSecretV1UpdateSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `update missing secret`() {
        val exception = catchThrowableOfType(
            { client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretString("bar")) },
            ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `change KMS key - no new version`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.updateSecret(UpdateSecretRequest().withSecretId(name).withKmsKeyId("secretKey"))

        assertThat(resp).isEqualTo(
            UpdateSecretResult()
                .withARN(secret.arn)
                .withName(name)
        )

        assertThat(secret.versions()).hasSize(1)
        assertThat(secret.kmsKeyId).isEqualTo("secretKey")
    }

    @Test
    fun `change string to binary - new version`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val newContent = ByteBuffer.wrap("baz".toByteArray())
        client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretBinary(newContent))

        assertThat(secret.versions()).hasSize(2)
        val latest = secret.latest()!!
        assertThat(latest.string).isNull()
        assertThat(latest.binary).isEqualTo(newContent)
    }

    @Test
    fun `update deleted secret`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val exception = catchThrowableOfType(
            { client.updateSecret(UpdateSecretRequest().withSecretId(secret.arn).withSecretString("baz")) },
            InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}