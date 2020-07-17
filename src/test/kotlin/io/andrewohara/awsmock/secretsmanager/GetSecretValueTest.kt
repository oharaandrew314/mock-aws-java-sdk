package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest
import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Test
import java.util.*

class GetSecretValueTest {

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
    fun `get missing secret`() {
        val exception = catchThrowableOfType(
                { client.getSecretValue(GetSecretValueRequest().withSecretId(UUID.randomUUID().toString())) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `get by name`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.getSecretValue(GetSecretValueRequest().withSecretId(name))

        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.secretString).isEqualTo("bar")
        assertThat(resp.secretBinary).isNull()
        assertThat(resp.versionId).isEqualTo(resp.versionId)
        assertThat(resp.versionStages).containsExactly("AWSCURRENT")
    }

    @Test
    fun `get by arn`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.getSecretValue(GetSecretValueRequest().withSecretId(created.arn))

        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.secretString).isEqualTo("bar")
        assertThat(resp.secretBinary).isNull()
        assertThat(resp.versionId).isEqualTo(resp.versionId)
        assertThat(resp.versionStages).containsExactly("AWSCURRENT")
    }
}