package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretsV1DescribeSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV1(backend)
    private val name = UUID.randomUUID().toString()

    @Test
    fun `describe missing secret`() {
        val exception = catchThrowableOfType(
            { client.describeSecret(DescribeSecretRequest().withSecretId(name)) },
            ResourceNotFoundException::class.java
        )

        exception.assertIsCorrect()
    }

    @Test
    fun `describe secret`() {
        val secret = backend.create(name, secretString = "bar", description = "secret stuff", kmsKeyId = "secretKey", tags = mapOf("Service" to "cats")).first
        val version2 = secret.add("baz", null)!!
        val version3 = secret.add("bang", null)!!

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(name))

        assertThat(resp).isEqualTo(
            DescribeSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withKmsKeyId("secretKey")
                .withDescription("secret stuff")
                .withTags(Tag().withKey("Service").withValue("cats"))
                .withVersionIdsToStages(mapOf(
                    version2.versionId to listOf("AWSPREVIOUS"),
                    version3.versionId to listOf("AWSCURRENT")
                ))
        )
    }
}