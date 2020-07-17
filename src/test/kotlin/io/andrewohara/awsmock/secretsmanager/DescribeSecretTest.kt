package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.SecretsUtils.assertIsCorrect
import org.assertj.core.api.Assertions.*
import org.junit.Test
import java.util.*

class DescribeSecretTest {

    private val client = MockAWSSecretsManager()
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
        client.createSecret(CreateSecretRequest()
                .withName(name)
                .withSecretString("bar")
                .withDescription("secret stuff")
                .withKmsKeyId("secretKey")
                .withTags(Tag().withKey("Service").withValue("cats"))
        )
        val previous = client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("baz"))
        val latest = client.putSecretValue(PutSecretValueRequest().withSecretId(name).withSecretString("bang"))

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(name))

        assertThat(resp.arn).isEqualTo(latest.arn)
        assertThat(resp.description).isEqualTo("secret stuff")
        assertThat(resp.kmsKeyId).isEqualTo("secretKey")
        assertThat(resp.tags).containsExactly(Tag().withKey("Service").withValue("cats"))
        assertThat(resp.versionIdsToStages).isEqualTo(mapOf(
                previous.versionId to listOf("AWSPREVIOUS"),
                latest.versionId to listOf("AWSCURRENT")
        ))
    }

    @Test
    fun `describe secret by arn with single version`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(existing.arn))

        assertThat(resp.arn).isEqualTo(existing.arn)
        assertThat(resp.description).isNull()
        assertThat(resp.kmsKeyId).isEqualTo("defaultKey")
        assertThat(resp.versionIdsToStages).isEqualTo(mapOf(
                existing.versionId to listOf("AWSCURRENT")
        ))
    }

    @Test
    fun `describe deleted secret`() {
        val existing = client.createSecret(CreateSecretRequest().withName(name).withSecretString("bar"))
        client.deleteSecret(DeleteSecretRequest().withSecretId(name))

        val resp = client.describeSecret(DescribeSecretRequest().withSecretId(name))

        assertThat(resp.arn).isEqualTo(existing.arn)
    }
}