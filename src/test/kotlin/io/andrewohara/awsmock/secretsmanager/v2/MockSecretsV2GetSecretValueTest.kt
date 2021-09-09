package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException

class MockSecretsV2GetSecretValueTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = "foo"

    @Test
    fun `get missing secret`() {
        assertThatThrownBy {
            client.getSecretValue {
                it.secretId("missingSecret")
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `get by name`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.getSecretValue {
            it.secretId(name)
        }

        assertThat(resp).isEqualTo(
            GetSecretValueResponse.builder()
                .arn(secret.arn)
                .name(name)
                .secretString("bar")
                .versionId("0")
                .versionStages("AWSCURRENT")
                .build()
        )
    }
}