package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.services.secretsmanager.model.*
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ListSecretsTest {

    private val client = MockAWSSecretsManager()

    @Test
    fun `list secrets`() {
        val secret1 = client.createSecret(CreateSecretRequest().withName("foo").withSecretString("bar"))
        val secret2 = client.createSecret(CreateSecretRequest()
                .withName("toll")
                .withSecretString("troll")
                .withKmsKeyId("secretKey")
                .withTags(Tag().withKey("Service").withValue("trolls"))
        )

        val resp = client.listSecrets(ListSecretsRequest())

        assertThat(resp.secretList).containsExactlyInAnyOrder(
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
    }

    @Test
    fun `list secrets with limit`() {
        client.createSecret(CreateSecretRequest().withName("foo").withSecretString("bar"))
        client.createSecret(CreateSecretRequest().withName("toll").withSecretString("troll"))

        val resp = client.listSecrets(ListSecretsRequest().withMaxResults(1))

        assertThat(resp.secretList).hasSize(1)
    }
}