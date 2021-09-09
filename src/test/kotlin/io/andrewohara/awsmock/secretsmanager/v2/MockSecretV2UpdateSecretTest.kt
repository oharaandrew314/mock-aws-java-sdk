package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretResponse

class MockSecretV2UpdateSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = "foo"

    @Test
    fun `update missing secret`() {
        assertThatThrownBy {
            client.putSecretValue {
                it.secretId(name)
                it.secretString("bar")
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `change KMS key - no new version`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.updateSecret {
            it.secretId(name)
            it.kmsKeyId("secretKey")
        }

        assertThat(resp).isEqualTo(
            UpdateSecretResponse.builder()
                .arn(secret.arn)
                .name(name)
                .build()
        )

        assertThat(secret.versions()).hasSize(1)
        assertThat(secret.kmsKeyId).isEqualTo("secretKey")
    }

    @Test
    fun `change string to binary - new version`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val newContent = SdkBytes.fromString("baz", Charsets.UTF_8)
        val resp = client.updateSecret {
            it.secretId(name)
            it.secretBinary(newContent)
        }

        assertThat(resp).isEqualTo(
            UpdateSecretResponse.builder()
                .arn(secret.arn)
                .name(name)
                .versionId("1")
                .build()
        )

        assertThat(secret.versions()).hasSize(2)
        val latest = secret.latest()!!
        assertThat(latest.string).isNull()
        assertThat(latest.binary).isEqualTo(newContent.asByteBuffer())
    }

    @Test
    fun `update deleted secret`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        assertThatThrownBy {
            client.updateSecret {
                it.secretId(secret.arn)
                it.secretString("baz")
            }
        }.isInstanceOf(SecretsManagerException::class.java)
    }
}