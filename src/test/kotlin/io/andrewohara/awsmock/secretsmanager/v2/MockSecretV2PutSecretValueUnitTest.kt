package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException

class MockSecretV2PutSecretValueUnitTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = "foo"

    @Test
    fun `put to missing secret`() {
        assertThatThrownBy {
            client.putSecretValue {
                it.secretId("missingSecret")
                it.secretString("bar")
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `put secret value`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.putSecretValue {
            it.secretId(name)
            it.secretString("baz")
        }

        assertThat(resp).isEqualTo(
            PutSecretValueResponse.builder()
                .arn(secret.arn)
                .name(secret.name)
                .versionId("1")
                .versionStages("AWSCURRENT")
                .build()
        )
    }

    @Test
    fun `put for deleted secret`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        assertThatThrownBy {
            client.putSecretValue {
                it.secretId(secret.arn)
                it.secretString("baz")
            }
        }.isInstanceOf(SecretsManagerException::class.java)
    }
}