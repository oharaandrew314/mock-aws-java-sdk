package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.ListSecretVersionIdsResponse
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.secretsmanager.model.SecretVersionsListEntry

class MockSecretsV2ListSecretVersionIdsTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = "my passwords"

    @Test
    fun `list for missing secret`() {
        assertThatThrownBy {
            client.listSecretVersionIds {
                it.secretId(name)
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `list for secret with single version`() {
        val (created, _) = backend.create(name, secretString = "bar")

        val resp = client.listSecretVersionIds {
            it.secretId(name)
        }

        assertThat(resp).isEqualTo(
            ListSecretVersionIdsResponse.builder()
                .arn(created.arn)
                .name(name)
                .versions(
                    SecretVersionsListEntry.builder()
                        .versionId("0")
                        .versionStages("AWSCURRENT")
                        .build()
                )
                .build()
        )
    }
}