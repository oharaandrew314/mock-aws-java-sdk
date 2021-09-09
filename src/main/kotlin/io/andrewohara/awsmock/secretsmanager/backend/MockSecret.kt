package io.andrewohara.awsmock.secretsmanager.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.nio.ByteBuffer

data class MockSecret(
    val name: String,
    var description: String?,
    val tags: Map<String, String>?,
    var kmsKeyId: String,
    private val history: MutableList<MockSecretValue> = mutableListOf(),
    var deleted: Boolean = false,
) {
    val arn = "arn:mockaws:secretsmanager:region:account-id:$name"

    fun latest() = history.lastOrNull()
    fun versions() = history.filter { it.stages.isNotEmpty() }.toList()

    fun add(string: String?, binary: ByteBuffer?): MockSecretValue? {
        if (deleted) throw cannotUpdateDeletedSecret()

        val version = when {
            string != null && binary != null -> throw MockAwsException(
                message = "You can't specify both a binary secret value and a string secret value in the same secret.",
                errorCode = "InvalidParameterException",
                statusCode = 400
            )
            string == null && binary == null -> return null
            else -> MockSecretValue(
                versionId = history.size.toString(),
                string = string,
                binary = binary,
                stages = listOf("AWSCURRENT")
            )
        }

        history.reversed().drop(1).firstOrNull()?.makeObsolete()
        history.lastOrNull()?.makePrevious()
        history.add(version)

        return version
    }

    fun update(
        description: String? = null,
        kmsKeyId: String? = null,
        secretString: String? = null,
        secretBinary: ByteBuffer? = null
    ): MockSecretValue? {
        if (deleted) throw cannotUpdateDeletedSecret()

        val version = add(secretString, secretBinary)
        this.description = description ?: this.description
        this.kmsKeyId = kmsKeyId ?: this.kmsKeyId

        return version
    }

    private fun cannotUpdateDeletedSecret() = MockAwsException(
        message = "You can't perform this operation on the secret because it was marked for deletion.",
        errorCode = "InvalidRequestException",
        statusCode = 400
    )
}