package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class ListSecretVersionIdsTest {

    private val client = MockAWSSecretsManager()
    private val name = UUID.randomUUID().toString()

    @Test
    fun `list for missing secret`() {
        val exception = catchThrowableOfType(
                { client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name)) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `list for secret with single version`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name))
        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versions).containsExactlyInAnyOrder(
                SecretVersionsListEntry().withVersionId(created.versionId).withVersionStages(listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `list for secret with many versions`() {
        val version1 = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        val version2 = client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("baz"))
        val version3 = client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("bang"))

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name))
        assertThat(resp.arn).isEqualTo(version1.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versions).containsExactly(
                SecretVersionsListEntry().withVersionId(version3.versionId).withVersionStages(listOf("AWSCURRENT")),
                SecretVersionsListEntry().withVersionId(version2.versionId).withVersionStages(listOf("AWSPREVIOUS")),
                SecretVersionsListEntry().withVersionId(version1.versionId).withVersionStages(listOf("AWSPREVIOUS"))
        )
    }

    @Test
    fun `list for secret by arn`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(created.arn))
        assertThat(resp.arn).isEqualTo(created.arn)
    }

    @Test
    fun `list for deleted secret`() {
        val created = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(created.arn))
        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versions).containsExactly(
                SecretVersionsListEntry().withVersionId(created.versionId).withVersionStages(listOf("AWSCURRENT"))
        )
    }
}