package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.v1.SecretsUtils.assertIsCorrect
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsManagerBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class MockSecretsV1DescribeSecretTest {

    private val backend = MockSecretsManagerBackend()
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
    fun `describe secret by name with 3 versions`() {
        val secret = backend.create(name, secretString = "bar", description = "secret stuff", kmsKeyId = "secretKey", tags = mapOf("Service" to "cats"))
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

    @Test
    fun `describe secret by arn with single version`() {
        val secret = backend.create(name, secretString = "bar")

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(secret.arn))

        assertThat(resp).isEqualTo(
            DescribeSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withKmsKeyId("defaultKey")
                .withVersionIdsToStages(mapOf(
                    secret.latest()!!.versionId to listOf("AWSCURRENT")
                ))
        )
    }

    @Test
    fun `describe deleted secret - should work`() {
        val secret = backend.create(name, secretString = "bar")
        backend.deleteSecret(secret.name)

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(name))

        assertThat(resp.arn).isEqualTo(secret.arn)
    }
}