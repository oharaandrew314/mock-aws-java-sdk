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

class MockSecretV1UpdateSecretTest {

    private val backend = MockSecretsManagerBackend()
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
        val secret = backend.create(name, secretString = "bar")

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
        val secret = backend.create(name, secretString = "bar")

        val newContent = ByteBuffer.wrap("baz".toByteArray())
        client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretBinary(newContent))

        assertThat(secret.versions()).hasSize(2)
        val latest = secret.latest()!!
        assertThat(latest.string).isNull()
        assertThat(latest.binary).isEqualTo(newContent)
    }

    @Test
    fun `change secret string by arn`() {
        val secret = backend.create(name, secretString = "bar")
        val previousVersionId = secret.latest()!!.versionId

        val resp = client.updateSecret(UpdateSecretRequest().withSecretId(secret.arn).withSecretString("baz"))
        val currentVersion = secret.latest()!!

        assertThat(secret.versions()).hasSize(2)
        assertThat(previousVersionId).isNotEqualTo(currentVersion.versionId)
        assertThat(currentVersion.string).isEqualTo("baz")
        assertThat(resp).isEqualTo(
            UpdateSecretResult()
                .withARN(secret.arn)
                .withName(name)
                .withVersionId(currentVersion.versionId)
        )
    }

    @Test
    fun `change description - no new version`() {
        val secret = backend.create(name, secretString = "bar")

        val resp = client.updateSecret(UpdateSecretRequest().withSecretId(name).withDescription("very descriptive"))
        assertThat(resp).isEqualTo(
            UpdateSecretResult()
                .withARN(secret.arn)
                .withName(name)
        )

        assertThat(secret.versions()).hasSize(1)
        assertThat(secret.description).isEqualTo("very descriptive")
    }

    @Test
    fun `change everything`() {
        val secret = backend.create(name, secretString = "bar")
        val previousVersionId = secret.latest()!!.versionId

        val resp = client.updateSecret(
            UpdateSecretRequest()
                .withSecretId(name)
                .withSecretString("baz")
                .withKmsKeyId("secretKey")
                .withDescription("very descriptive")
        )
        val currentVersion = secret.latest()!!

        assertThat(previousVersionId).isNotEqualTo(currentVersion.versionId)
        assertThat(secret.versions()).hasSize(2)
        assertThat(resp).isEqualTo(
            UpdateSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersionId(currentVersion.versionId)
        )

        assertThat(secret.description).isEqualTo("very descriptive")
        assertThat(secret.kmsKeyId).isEqualTo("secretKey")
        assertThat(secret.tags).isNull()
        assertThat(currentVersion.string).isEqualTo("baz")
    }

    @Test
    fun `changing one field doesn't null the others`() {
        client.createSecret(
            CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
                .withKmsKeyId("secretKey")
                .withDescription("secret stuff")
        )

        client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretString("baz"))

        val metadata = client.describeSecret(DescribeSecretRequest().withSecretId(name))
        assertThat(metadata.description).isEqualTo("secret stuff")
        assertThat(metadata.kmsKeyId).isEqualTo("secretKey")
    }

    @Test
    fun `update deleted secret`() {
        val secret = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val exception = catchThrowableOfType(
            { client.updateSecret(UpdateSecretRequest().withSecretId(secret.arn).withSecretString("baz")) },
            InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}