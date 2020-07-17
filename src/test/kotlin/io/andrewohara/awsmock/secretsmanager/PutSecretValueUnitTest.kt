package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.cannotUpdateDeletedSecret
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

class PutSecretValueUnitTest {

    private val client = MockAWSSecretsManager()
    private val name = UUID.randomUUID().toString()

    @Test
    fun `put missing secret`() {
        val exception = catchThrowableOfType(
                { client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("foo")) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `put existing string by name`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.putSecretValue(PutSecretValueRequest()
                .withSecretId(name)
                .withSecretString("baz")
        )

        assertThat(resp.arn).isEqualTo(existing.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNotEmpty().isNotEqualTo(existing.versionId)
        assertThat(resp.versionStages).containsExactly("AWSCURRENT")
    }

    @Test
    fun `put existing binary by name`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.putSecretValue(PutSecretValueRequest()
                .withSecretId(name)
                .withSecretBinary(ByteBuffer.wrap("baz".toByteArray()))
        )

        assertThat(resp.arn).isEqualTo(existing.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNotEmpty().isNotEqualTo(existing.versionId)
        assertThat(resp.versionStages).containsExactly("AWSCURRENT")
    }

    @Test
    fun `put existing string by arn`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.putSecretValue(PutSecretValueRequest()
                .withSecretId(existing.arn)
                .withSecretString("baz")
        )

        assertThat(resp.arn).isEqualTo(existing.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNotEmpty().isNotEqualTo(existing.versionId)
        assertThat(resp.versionStages).containsExactly("AWSCURRENT")
    }

    @Test
    fun `put for deleted secret`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        val exception = catchThrowableOfType(
                { client.putSecretValue(PutSecretValueRequest().withSecretId(existing.arn).withSecretString("baz")) },
                InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}