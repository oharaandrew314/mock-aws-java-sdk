package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsManagerBackend
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class MockSecretsV1ListSecretsTest {

    private val backend = MockSecretsManagerBackend()
    private val client = MockSecretsManagerV1(backend)

    @Test
    fun `list secrets`() {
        val secret1 = backend.create("foo", secretString = "bar")
        val secret2 = backend.create("toll", secretString = "troll", kmsKeyId = "secretKey", tags = mapOf("Service" to "trolls"))

        val resp = client.listSecrets(ListSecretsRequest())

        assertThat(resp).isEqualTo(
            ListSecretsResult()
                .withSecretList(
                    SecretListEntry()
                        .withARN(secret1.arn)
                        .withName("foo")
                        .withKmsKeyId("defaultKey"),
                    SecretListEntry()
                        .withARN(secret2.arn)
                        .withName("toll")
                        .withKmsKeyId("secretKey")
                        .withTags(Tag().withKey("Service").withValue("trolls"))
                )
        )
    }

    @Test
    fun `list secrets with limit`() {
        backend.create("foo", secretString = "bar")
        backend.create("toll", secretString = "troll")

        val resp = client.listSecrets(ListSecretsRequest().withMaxResults(1))

        assertThat(resp.secretList).hasSize(1)
    }
}