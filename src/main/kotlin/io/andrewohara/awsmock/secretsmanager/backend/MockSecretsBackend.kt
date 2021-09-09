package io.andrewohara.awsmock.secretsmanager.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.nio.ByteBuffer

class MockSecretsBackend {

    companion object {
        private const val defaultKmsKeyId = "defaultKey"
    }

    private val secrets = mutableSetOf<MockSecret>()
    fun secrets(limit: Int? = null) = secrets.take(limit ?: Int.MAX_VALUE).toList()

    operator fun get(id: String) = secrets.firstOrNull { it.arn == id || it.name == id }
    fun describeSecret(secretId: String) = get(secretId) ?: throw resourceNotFound()

    fun create(
        name: String?,
        description: String? = null,
        kmsKeyId: String? = null,
        tags: Map<String, String>? = null,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): Pair<MockSecret, MockSecretValue?> {
        if (name == null) throw MockAwsException(400, "ValidationException", "1 validation error detected: Value null at 'name' failed to satisfy constraint: Member must not be null")

        val existing = get(name)
        if (existing != null && !existing.deleted) {
            throw MockAwsException(400, "ResourceExistsException", "The operation failed because the secret $name already exists.")
        } else if (existing != null && existing.deleted) {
            throw MockAwsException(400, "InvalidRequestException", "You can't create this secret because a secret with this name is already scheduled for deletion.")
        }

        val secret = MockSecret(
            name = name,
            description = description,
            kmsKeyId = kmsKeyId ?: defaultKmsKeyId,
            tags = tags,
        )

        val version = secret.add(secretString, secretBinary)
        secrets += secret

        return secret to version
    }

    fun deleteSecret(secretId: String): MockSecret {
        val secret = get(secretId) ?: throw resourceNotFound()
        secret.deleted = true

        return secret
    }

    fun updateSecret(
        secretId: String,
        description: String? = null,
        kmsKeyId: String? = null,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): Pair<MockSecret, MockSecretValue?> {
        val secret = get(secretId) ?: throw resourceNotFound()
        val version = secret.update(
            description = description,
            kmsKeyId = kmsKeyId,
            secretString = secretString,
            secretBinary = secretBinary
        )

        return secret to version
    }

    fun putSecretValue(
        secretId: String,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): Pair<MockSecret, MockSecretValue?> {
        val secret = get(secretId) ?: throw resourceNotFound()
        val version = secret.add(secretString, secretBinary)
        return secret to version
    }

    private fun resourceNotFound() = MockAwsException(
        message = "Secrets Manager can't find the specified secret.",
        errorCode = "ResourceNotFoundException",
        statusCode = 400
    )
}