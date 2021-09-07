package io.andrewohara.awsmock.secretsmanager.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.nio.ByteBuffer

class MockSecretsManagerBackend {

    companion object {
        private const val defaultKmsKeyId = "defaultKey"
    }

    private val secrets = mutableSetOf<MockSecret>()
    fun secrets(limit: Int?) = secrets.take(limit ?: Int.MAX_VALUE).toList()

    operator fun get(id: String) = secrets.firstOrNull { it.arn == id || it.name == id }
    fun describeSecret(secretId: String) = get(secretId) ?: throw resourceNotFound()

    fun create(
        name: String?,
        description: String? = null,
        kmsKeyId: String? = null,
        tags: Map<String, String>? = null,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): MockSecret {
        if (name == null) throw MockAwsException(400, "ValidationException", "1 validation error detected: Value null at 'name' failed to satisfy constraint: Member must not be null")

        val existing = get(name)
        if (existing != null && !existing.deleted) {
            throw MockAwsException(400, "ResourceExistsException", "The operation failed because the secret $name already exists.")
        } else if (existing != null && existing.deleted) {
            throw MockAwsException(400, "InvalidRequestException", "You can't create this secret because a secret with this name is already scheduled for deletion.")
        }

        return MockSecret(
            name = name,
            description = description,
            kmsKeyId = kmsKeyId ?: defaultKmsKeyId,
            tags = tags,
        ).also { secret ->
            secret.add(secretString, secretBinary)
            secrets += secret
        }
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
        if (secret.deleted) throw cannotUpdateDeletedSecret()

        secret.description = description ?: secret.description
        secret.kmsKeyId = kmsKeyId ?: secret.kmsKeyId
        val version = secret.add(secretString, secretBinary)

        return secret to version
    }

    fun putSecretValue(
        secretId: String,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): MockSecret {
        val secret = get(secretId) ?: throw resourceNotFound()
        if (secret.deleted) throw cannotUpdateDeletedSecret()

        secret.add(secretString, secretBinary)

        return secret
    }

    private fun resourceNotFound() = MockAwsException(
        message = "Secrets Manager can't find the specified secret.",
        errorCode = "ResourceNotFoundException",
        statusCode = 400
    )

    private fun cannotUpdateDeletedSecret() = MockAwsException(
        message = "You can't perform this operation on the secret because it was marked for deletion.",
        errorCode = "InvalidRequestException",
        statusCode = 400
    )
}