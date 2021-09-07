package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.cannotUpdateDeletedSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsManagerBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class MockSecretV1PutSecretValueUnitTest {

    private val backend = MockSecretsManagerBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `put to missing secret`() {
        val exception = catchThrowableOfType(
            { client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("foo")) },
            ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `put string by name`() {
        val secret = backend.create(name, secretString = "bar")
        val previousVersionId = secret.latest()!!.versionId

        val resp = client.putSecretValue(
            PutSecretValueRequest()
                .withSecretId(name)
                .withSecretString("baz")
        )
        val currentVersionId = secret.latest()!!.versionId

        assertThat(previousVersionId).isNotEqualTo(currentVersionId)
        assertThat(resp).isEqualTo(
            PutSecretValueResult()
                .withARN(secret.arn)
                .withName(name)
                .withVersionId(currentVersionId)
                .withVersionStages("AWSCURRENT")
        )
    }

    @Test
    fun `put binary by name`() {
        val secret = backend.create(name, secretString = "bar")
        val previousVersionId = secret.latest()!!.versionId

        val resp = client.putSecretValue(
            PutSecretValueRequest()
                .withSecretId(name)
                .withSecretBinary(ByteBuffer.wrap("baz".toByteArray()))
        )
        val currentVersionId = secret.latest()!!.versionId

        assertThat(previousVersionId).isNotEqualTo(currentVersionId)
        assertThat(resp).isEqualTo(
            PutSecretValueResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersionId(currentVersionId)
                .withVersionStages("AWSCURRENT")
        )
    }

    @Test
    fun `put string by arn`() {
        val secret = backend.create(name, secretString = "bar")
        val previousVersionId = secret.latest()!!.versionId

        val resp = client.putSecretValue(
            PutSecretValueRequest()
                .withSecretId(secret.arn)
                .withSecretString("baz")
        )
        val currentVersionId = secret.latest()!!.versionId

        assertThat(previousVersionId).isNotEqualTo(currentVersionId)
        assertThat(resp).isEqualTo(
            PutSecretValueResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersionId(currentVersionId)
                .withVersionStages("AWSCURRENT")
        )
    }

    @Test
    fun `put for deleted secret`() {
        val secret = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val exception = catchThrowableOfType(
            { client.putSecretValue(PutSecretValueRequest().withSecretId(secret.arn).withSecretString("baz")) },
            InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}