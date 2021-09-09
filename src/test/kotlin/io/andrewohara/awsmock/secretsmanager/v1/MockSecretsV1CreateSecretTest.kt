package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.backend.MockSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretValue
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertCantGiveBothTypes
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertParamNotNullable
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.cannotCreateDeletedSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.*

class MockSecretsV1CreateSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `create secret without any params`() {
        val exception = catchThrowableOfType(
            { client.createSecret(CreateSecretRequest()) },
            AWSSecretsManagerException::class.java
        )

        exception.assertParamNotNullable("name")
    }

    @Test
    fun `create secret without content`() {
        val result = client.createSecret(CreateSecretRequest().withName(name))

        assertThat(result.name).isEqualTo(name)
        backend[result.name]!!.versions().isEmpty()
    }

    @Test
    fun `create secret without name`() {
        val exception = catchThrowableOfType(
            { client.createSecret(CreateSecretRequest().withSecretString("bar")) },
            AWSSecretsManagerException::class.java
        )

        exception.assertParamNotNullable("name")
    }

    @Test
    fun `create secret`() {
        val resp = client.createSecret(
            CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
                .withTags(Tag().withKey("key").withValue("value"))
        )

        assertThat(resp).isEqualTo(
            CreateSecretResult()
                .withARN("arn:mockaws:secretsmanager:region:account-id:$name")
                .withName(name)
                .withVersionId("0")
        )

        assertThat(backend[name]).isEqualTo(
            MockSecret(
                name = name,
                description = null,
                tags = mapOf("key" to "value"),
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

        val exception = catchThrowableOfType(
            { client.createSecret(CreateSecretRequest().withName(name).withSecretString("baz")) },
            ResourceExistsException::class.java
        )

        exception.assertIsCorrect(name)
    }

    @Test
    fun `can't create secret that was scheduled for deletion`() {
        backend.create(name, secretString = "bar")
        backend.deleteSecret(name)

        val exception = catchThrowableOfType(
            { client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar")) },
            InvalidRequestException::class.java
        )

        exception.cannotCreateDeletedSecret()
    }

    @Test
    fun `create secret with string and binary content`() {
        val exception = catchThrowableOfType(
            {
                client.createSecret(
                    CreateSecretRequest().withName(name).withSecretString("bar")
                        .withSecretBinary(ByteBuffer.wrap("baz".toByteArray()))
                )
            },
            InvalidParameterException::class.java
        )

        exception.assertCantGiveBothTypes()
    }
}