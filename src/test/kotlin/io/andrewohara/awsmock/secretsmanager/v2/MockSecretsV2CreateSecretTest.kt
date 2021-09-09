package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretValue
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.secretsmanager.model.*
import java.util.*

class MockSecretsV2CreateSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `create secret without any params`() {
        assertThatThrownBy {
            client.createSecret { }
        }.isInstanceOf(SecretsManagerException::class.java)
    }

    @Test
    fun `create secret without content`() {
        val result = client.createSecret {
            it.name(name)
        }

        assertThat(result).isEqualTo(
            CreateSecretResponse.builder()
                .arn("arn:mockaws:secretsmanager:region:account-id:$name")
                .name(name)
                .build()
        )
    }

    @Test
    fun `create secret without name`() {
        assertThatThrownBy {
            client.createSecret {
                it.secretString("bar")
            }
        }.isInstanceOf(SecretsManagerException::class.java)
    }

    @Test
    fun `create secret`() {
        val resp = client.createSecret {
            it.name(name)
            it.secretString("bar")
            it.tags(Tag.builder().key("tag").value("value").build())
        }

        assertThat(resp).isEqualTo(
            CreateSecretResponse.builder()
                .arn("arn:mockaws:secretsmanager:region:account-id:$name")
                .name(name)
                .versionId("0")
                .build()
        )

        assertThat(backend[name]).isEqualTo(
            MockSecret(
                name = name,
                description = null,
                tags = mapOf("tag" to "value"),
                kmsKeyId = "defaultKey",
                history = mutableListOf(
                    MockSecretValue("0", "bar", null, listOf("AWSCURRENT"))
                )
            )
        )
    }

    @Test
    fun `create secret that already exists`() {
        backend.create(name, secretString = "bar")

        assertThatThrownBy {
            client.createSecret {
                it.name(name)
                it.secretString("baz")
            }
        }.isInstanceOf(ResourceExistsException::class.java)
    }

    @Test
    fun `can't create secret that was scheduled for deletion`() {
        backend.create(name, secretString = "bar")
        backend.deleteSecret(name)

        assertThatThrownBy {
            client.createSecret {
                it.name(name)
                it.secretString("bar")
            }
        }.isInstanceOf(InvalidRequestException::class.java)
    }

    @Test
    fun `create secret with string and binary content`() {
        assertThatThrownBy {
            client.createSecret {
                it.name(name)
                it.secretString("bar")
                it.secretBinary(SdkBytes.fromString("baz", Charsets.UTF_8))
            }
        }.isInstanceOf(InvalidParameterException::class.java)
    }
}