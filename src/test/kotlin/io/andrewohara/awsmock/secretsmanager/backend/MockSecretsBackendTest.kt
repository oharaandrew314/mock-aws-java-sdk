package io.andrewohara.awsmock.secretsmanager.backend

import io.andrewohara.awsmock.core.MockAwsException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class MockSecretsBackendTest {

    private val backend = MockSecretsBackend()
    private val name = "foo"

    @Test
    fun `create secret with all parameters`() {
        backend.create(
            name = name,
            description = "stuff",
            kmsKeyId = "secretKey",
            secretString = "bar",
            tags = mapOf("tag" to "value")
        )

        assertThat(backend.secrets()).containsExactly(
            MockSecret(
                name = name,
                description = "stuff",
                kmsKeyId = "secretKey",
                tags = mapOf("tag" to "value"),
                history = mutableListOf(
                    MockSecretValue("0", "bar", null, listOf("AWSCURRENT"))
                )
            )
        )
    }

    @Test
    fun `list versions for secret with single version`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        assertThat(secret.versions()).containsExactly(
            MockSecretValue("0", "bar", null, listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `list versions for secret with many - with original being obsolete`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        secret.add("baz", null)!!
        secret.add("bang", null)!!

        assertThat(secret.versions()).containsExactly(
            MockSecretValue("1", "baz", null, listOf("AWSPREVIOUS")),
            MockSecretValue("2", "bang", null, listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `list versions for deleted secret - will succeed`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        assertThat(secret.versions()).containsExactly(
            MockSecretValue("0", "bar", null, listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `updating single field field doesn't null the others`() {
        val secret = backend.create(name, secretString = "bar", kmsKeyId = "secretKey", description = "secret stuff").first

        backend.updateSecret(secretId = secret.arn, description = "updated")

        assertThat(secret.latest()?.string).isEqualTo("bar")
        assertThat(secret.kmsKeyId).isEqualTo("secretKey")
        assertThat(secret.description).isEqualTo("updated")
    }


    @Test
    fun `update secret with new value and description`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        secret.update(
            description = "descriptive",
            kmsKeyId = "secretKey",
            secretString = "baz"
        )

        assertThat(secret).isEqualTo(MockSecret(
            name = name,
            description = "descriptive",
            kmsKeyId = "secretKey",
            tags = null,
            history = mutableListOf(
                MockSecretValue("0", "bar", null, listOf("AWSPREVIOUS")),
                MockSecretValue("1", "baz", null, listOf("AWSCURRENT"))
            )
        ))
    }

    @Test
    fun `put binary value`() {
        val binary = ByteBuffer.wrap("baz".toByteArray())

        val (secret, _) = backend.create(name, secretString = "bar")
        secret.add(null, binary)

        assertThat(secret.versions()).containsExactly(
            MockSecretValue("0", "bar", null, listOf("AWSPREVIOUS")),
            MockSecretValue("1", null, binary, listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `put string value`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        secret.add("baz", null)

        assertThat(secret.versions()).containsExactly(
            MockSecretValue("0", "bar", null, listOf("AWSPREVIOUS")),
            MockSecretValue("1", "baz", null, listOf("AWSCURRENT"))
        )
    }

    @Test
    fun `put secret value with no value - should do nothing`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        assertThat(secret.add(null, null)).isNull()

        assertThat(secret.versions()).hasSize(1)
    }

    @Test
    fun `put string and binary value`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        assertThatThrownBy {
            secret.add("baz", ByteBuffer.wrap("baz".toByteArray()))
        }.isInstanceOf(MockAwsException::class.java)
    }

    @Test
    fun `delete secret by arn`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        assertThat(backend.deleteSecret(secret.arn))
            .isEqualTo(secret)

        assertThat(backend[secret.name]?.deleted).isTrue
    }

    @Test
    fun `delete secret by name`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        assertThat(backend.deleteSecret(secret.name))
            .isEqualTo(secret)

        assertThat(backend[secret.name]?.deleted).isTrue
    }

    @Test
    fun `delete deleted secret - no error`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        assertThat(backend.deleteSecret(secret.name))
            .isEqualTo(secret)
    }
}