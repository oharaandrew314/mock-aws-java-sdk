package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.cannotUpdateDeletedSecret
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretV1PutSecretValueUnitTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `put to missing secret`() {
        val exception = catchThrowableOfType(
            { client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("foo")) },
            ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `put secret value`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.putSecretValue(
            PutSecretValueRequest()
                .withSecretId(name)
                .withSecretString("baz")
        )

        assertThat(resp).isEqualTo(
            PutSecretValueResult()
                .withARN(secret.arn)
                .withName(name)
                .withVersionId("1")
                .withVersionStages("AWSCURRENT")
        )
    }

    @Test
    fun `put for deleted secret`() {
        val (secret, _) = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val exception = catchThrowableOfType(
            { client.putSecretValue(PutSecretValueRequest().withSecretId(secret.arn).withSecretString("baz")) },
            InvalidRequestException::class.java
        )

        exception.cannotUpdateDeletedSecret()
    }
}