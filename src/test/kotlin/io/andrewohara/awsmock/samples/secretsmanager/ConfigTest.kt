package io.andrewohara.awsmock.samples.secretsmanager

import com.amazonaws.services.secretsmanager.model.CreateSecretRequest
import io.andrewohara.awsmock.secretsmanager.MockSecretsManagerV1
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ConfigTest {

    private val client = MockSecretsManagerV1()

    @Test
    fun `load config`() {
        // initialize state
        client.createSecret(CreateSecretRequest().withName("cats-dev-tableName").withSecretString("kitties-dev"))
        client.createSecret(CreateSecretRequest().withName("cats-dev-queueUrl").withSecretString("https://queues.fake/meows"))

        Assertions.assertThat(Config.fromSecrets(client)).isEqualTo(Config(
                tableName = "kitties-dev",
                queueUrl = "https://queues.fake/meows"
        ))
    }
}