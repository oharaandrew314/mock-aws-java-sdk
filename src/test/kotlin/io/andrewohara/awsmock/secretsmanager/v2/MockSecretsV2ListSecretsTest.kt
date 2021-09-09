package io.andrewohara.awsmock.secretsmanager.v2

import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV2
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry
import software.amazon.awssdk.services.secretsmanager.model.Tag

class MockSecretsV2ListSecretsTest {

    private val backend = MockSecretsBackend()
    private val client = MockSecretsManagerV2(backend)

    @Test
    fun `list secrets`() {
        val (secret1, _) = backend.create("foo", secretString = "bar")
        val secret2 = backend.create("toll", secretString = "troll", kmsKeyId = "secretKey", tags = mapOf("Service" to "trolls")).first

        val resp = client.listSecrets()

        assertThat(resp).isEqualTo(
            ListSecretsResponse.builder()
                .secretList(
                    SecretListEntry.builder()
                        .arn(secret1.arn)
                        .name("foo")
                        .kmsKeyId("defaultKey")
                        .build(),
                    SecretListEntry.builder()
                        .arn(secret2.arn)
                        .name("toll")
                        .kmsKeyId("secretKey")
                        .tags(Tag.builder().key("Service").value("trolls").build())
                        .build()
                )
                .build()
        )
    }

    @Test
    fun `list secrets with limit`() {
        backend.create("foo", secretString = "bar")
        backend.create("toll", secretString = "troll")

        val resp = client.listSecrets {
            it.maxResults(1)
        }

        assertThat(resp.secretList()).hasSize(1)
    }
}