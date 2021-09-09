package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretsV1GetSecretValueTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `get missing secret`() {
        val exception = catchThrowableOfType(
            { client.getSecretValue(GetSecretValueRequest().withSecretId(UUID.randomUUID().toString())) },
            ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `get by name`() {
        val (secret, _) = backend.create(name, secretString = "bar")

        val resp = client.getSecretValue(GetSecretValueRequest().withSecretId(name))

        assertThat(resp).isEqualTo(
            GetSecretValueResult()
                .withARN(secret.arn)
                .withName(name)
                .withSecretString("bar")
                .withVersionId(secret.latest()!!.versionId)
                .withVersionStages("AWSCURRENT")
        )
    }
}