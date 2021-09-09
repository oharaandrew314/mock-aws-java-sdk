package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.secretsmanager.model.Tag

class MockSecretsV2DescribeSecretTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)
    private val name = "foo"

    @Test
    fun `describe missing secret`() {
        assertThatThrownBy {
            client.describeSecret {
                it.secretId(name)
            }
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `describe secret`() {
        val secret = backend.create(name, secretString = "bar", description = "secret stuff", kmsKeyId = "secretKey", tags = mapOf("Service" to "cats")).first
        secret.add("baz", null)!!
        secret.add("bang", null)!!

        val resp = client.describeSecret {
            it.secretId(name)
        }

        assertThat(resp).isEqualTo(
            DescribeSecretResponse.builder()
                .arn("arn:mockaws:secretsmanager:region:account-id:$name")
                .name(name)
                .kmsKeyId("secretKey")
                .description("secret stuff")
                .tags(Tag.builder().key("Service").value("cats").build())
                .versionIdsToStages(mapOf(
                    "1" to listOf("AWSPREVIOUS"),
                    "2" to listOf("AWSCURRENT")
                ))
                .build()
        )
    }
}