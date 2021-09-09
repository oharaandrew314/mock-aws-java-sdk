package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSecretsV1ListSecretVersionIdsTest {

    private val backend = MockSecretsBackend()
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
        val (created, _) = backend.create(name, secretString = "bar")

        val resp = client.listSecretVersionIds(ListSecretVersionIdsRequest().withSecretId(name))
        assertThat(resp.arn).isEqualTo(created.arn)
        assertThat(resp.name).isEqualTo(name)
        assertThat(created.latest()).isNotNull
        assertThat(resp.versions).containsExactlyInAnyOrder(
            SecretVersionsListEntry().withVersionId(created.latest()!!.versionId).withVersionStages(listOf("AWSCURRENT"))
        )
    }
}