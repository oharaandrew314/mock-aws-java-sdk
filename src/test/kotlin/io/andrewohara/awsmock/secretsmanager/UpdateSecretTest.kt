package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.cannotUpdateDeletedSecret
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class UpdateSecretTest {

    private val client = MockAWSSecretsManager()
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
    fun `change key and assert response data`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.updateSecret(UpdateSecretRequest().withSecretId(name).withKmsKeyId("secretKey"))

        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNull()

        val retrieved = client.describeSecret(DescribeSecretRequest().withSecretId(name))
        assertThat(retrieved.kmsKeyId).isEqualTo("secretKey")
    }

    @Test
    fun `change string to binary`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val newContent = ByteBuffer.wrap("baz".toByteArray())
        client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretBinary(newContent))

        assertThat(client.getSecretValue(GetSecretValueRequest().withSecretId(name)).secretBinary).isEqualTo(newContent)
    }

    @Test
    fun `change string by arn`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        client.updateSecret(UpdateSecretRequest().withSecretId(created.arn).withSecretString("baz"))

        assertThat(client.getSecretValue(GetSecretValueRequest().withSecretId(name)).secretString).isEqualTo("baz")
    }

    @Test
    fun `change description`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        client.updateSecret(UpdateSecretRequest().withSecretId(name).withDescription("very descriptive"))

        assertThat(client.describeSecret(DescribeSecretRequest().withSecretId(name)).description).isEqualTo("very descriptive")
    }

    @Test
    fun `change everything`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        client.updateSecret(UpdateSecretRequest()
                .withSecretId(name)
                .withSecretString("baz")
                .withKmsKeyId("secretKey")
                .withDescription("very descriptive")
        )

        val retrieved = client.describeSecret(DescribeSecretRequest().withSecretId(name))
        assertThat(retrieved.description).isEqualTo("very descriptive")
        assertThat(retrieved.kmsKeyId).isEqualTo("secretKey")
        assertThat(retrieved.tags).isNull()
        assertThat(client.getSecretValue(GetSecretValueRequest().withSecretId(name)).secretString).isEqualTo("baz")
    }

    @Test
    fun `updating does not return new versionId and does not add new version to history`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.updateSecret(UpdateSecretRequest().withSecretId(name).withSecretString("baz"))

        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNull()

        assertThat(client.describeSecret(DescribeSecretRequest().withSecretId(name)).versionIdsToStages).isEqualTo(mapOf(
                created.versionId to listOf("AWSCURRENT")
        ))
    }

    @Test
    fun `changing one field doesn't null the others`() {
        client.createSecret(CreateSecretRequest()
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
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        val exception = catchThrowableOfType(
                { client.updateSecret(UpdateSecretRequest().withSecretId(existing.arn).withSecretString("baz")) },
                InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}