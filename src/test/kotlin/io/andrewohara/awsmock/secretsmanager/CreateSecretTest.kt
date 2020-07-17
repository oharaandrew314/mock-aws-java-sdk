package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertCantGiveBothTypes
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertParamNotNullable
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.cannotCreateDeletedSecret
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

class CreateSecretTest {

    private val client = MockAWSSecretsManager()
    private val name = UUID.randomUUID().toString()

    @After
    fun cleanup() {
        try {
            client.deleteSecret(DeleteSecretRequest().withSecretId(name))
        } catch (e: ResourceNotFoundException) {
            // no-op
        }
    }

    @Test
    fun `create secret without any params`() {
        val exception = catchThrowableOfType(
                { client.createSecret(CreateSecretRequest()) },
                AWSSecretsManagerException::class.java
        )

        exception.assertParamNotNullable("name")
    }

    @Test
    fun `create secret without content`() {
        client.createSecret(CreateSecretRequest().withName(name))
    }

    @Test
    fun `create secret without name`() {
        val exception = catchThrowableOfType(
                { client.createSecret(CreateSecretRequest().withSecretString("bar")) },
                AWSSecretsManagerException::class.java
        )

        exception.assertParamNotNullable("name")
    }

    @Test
    fun `create binary secret`() {
        val resp = client.createSecret(CreateSecretRequest()
                .withName(name)
                .withSecretBinary(ByteBuffer.wrap("bar".toByteArray()))
        )

        assertThat(resp.arn).isEqualTo("arn:mockaws:secretsmanager:region:account-id:$name")
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNotEmpty()
    }

    @Test
    fun `create string secret`() {
        val resp = client.createSecret(CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
        )

        assertThat(resp.arn).isEqualTo("arn:mockaws:secretsmanager:region:account-id:$name")
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versionId).isNotEmpty()
    }

    @Test
    fun `create string secret with kms key`() {
        client.createSecret(CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
                .withKmsKeyId("secretKey")
        )
    }

    @Test
    fun `create secret that already exists`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val exception = catchThrowableOfType(
                { client.createSecret(CreateSecretRequest().withName(name).withSecretString("baz")) },
                ResourceExistsException::class.java
        )

        exception.assertIsCorrect(name)
    }

    @Test
    fun `can't create secret that was scheduled for deletion`() {
        client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        val exception = catchThrowableOfType(
                { client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar")) },
                InvalidRequestException::class.java
        )

        exception.cannotCreateDeletedSecret()
    }

    @Test
    fun `create secret with string and binary content`() {
        val exception = catchThrowableOfType(
                { client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar").withSecretBinary(ByteBuffer.wrap("baz".toByteArray()))) },
                InvalidParameterException::class.java
        )

        exception.assertCantGiveBothTypes()
    }

    @Test
    fun `create secret with tags`() {
        client.createSecret(CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
                .withTags(Tag().withKey("Service").withValue("cats"))
        )
    }
}