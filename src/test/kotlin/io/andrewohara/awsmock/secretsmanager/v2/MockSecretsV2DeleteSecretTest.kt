package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretResponse
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException

class MockSecretsV2DeleteSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)

    @Test
    fun `delete missing secret`() {
        assertThatThrownBy {
            client.deleteSecret {
                it.secretId("foo")
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `delete secret`() {
        val (secret, _) = backend.create("foo", secretString = "bar")

        val resp = client.deleteSecret {
            it.secretId(secret.arn)
        }

        assertThat(resp).isEqualTo(
            DeleteSecretResponse.builder()
                .arn(secret.arn)
                .name(secret.name)
                .build()
        )

        assertThat(backend[secret.name]?.deleted).isTrue
    }
}