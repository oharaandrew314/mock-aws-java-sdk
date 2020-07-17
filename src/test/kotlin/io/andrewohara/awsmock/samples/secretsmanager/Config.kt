package io.andrewohara.awsmock.samples.secretsmanager

import com.amazonaws.services.secretsmanager.AWSSecretsManager
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest

data class Config(
        val tableName: String,
        val queueUrl: String
) {
    companion object {
        fun fromSecrets(client: AWSSecretsManager): Config {
            return Config(
                    tableName = client.getSecretValue(GetSecretValueRequest().withSecretId("cats-dev-tableName")).secretString,
                    queueUrl = client.getSecretValue(GetSecretValueRequest().withSecretId("cats-dev-queueUrl")).secretString
            )
        }
    }
}