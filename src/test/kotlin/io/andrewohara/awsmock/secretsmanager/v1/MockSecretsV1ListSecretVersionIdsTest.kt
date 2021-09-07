package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsManagerBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSecretsV1ListSecretVersionIdsTest {

    private val backend = MockSecretsManagerBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = "my passwords"

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
        val created = backend.create(name, secretString = "bar")

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name))
        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(created.latest()).isNotNull
        assertThat(resp.versions).containsExactlyInAnyOrder(
            SecretVersionsListEntry().withVersionId(created.latest()!!.versionId).withVersionStages(listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `list for secret with many - with original being obsolete`() {
        val secret = backend.create(name, secretString = "bar")
        val version2 = secret.add("baz", null)!!
        val version3 = secret.add("bang", null)!!

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name))

        assertThat(resp).isEqualTo(
            ListSecretVersionIdsResult()
                .withARN(secret.arn)
                .withName(name)
                .withVersions(
                    SecretVersionsListEntry().withVersionId(version3.versionId).withVersionStages(listOf("AWSCURRENT")),
                    SecretVersionsListEntry().withVersionId(version2.versionId).withVersionStages(listOf("AWSPREVIOUS")),
                )
        )
    }

    @Test
    fun `list for secret by arn`() {
        val secret = backend.create(name, secretString = "bar")

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(secret.arn))
        assertThat(resp.name).isEqualTo(secret.name)
    }

    @Test
    fun `list for deleted secret - will succeed`() {
        val secret = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(secret.arn))
        assertThat(resp.arn).isEqualTo(secret.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(resp.versions).containsExactly(
            SecretVersionsListEntry().withVersionId(secret.latest()!!.versionId).withVersionStages(listOf("AWSCURRENT"))
        )
    }
}