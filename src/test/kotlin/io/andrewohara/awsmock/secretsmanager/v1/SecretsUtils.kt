package io.andrewohara.awsmock.secretsmanager.v1

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.secretsmanager.AWSSecretsManager
import com.amazonaws.services.secretsmanager.model.*
import org.assertj.core.api.Assertions.*
import java.nio.ByteBuffer

object SecretsUtils {

    fun AWSSecretsManager.delete(id: String) {
        try {
            deleteSecret(DeleteSecretRequest().withSecretId(id))
        } catch (e: ResourceNotFoundException) {
            // no-op
        }
    }

    operator fun AWSSecretsManager.set(name: String, data: SecretData) {
        try {
            createSecret(CreateSecretRequest()
                    .withName(name)
                    .withSecretBinary(data.binaryContent)
                    .withSecretString(data.stringContent)
                    .withKmsKeyId(data.kmsKeyId)
            )
        } catch (e: ResourceExistsException) {
            updateSecret(UpdateSecretRequest()
                    .withSecretId(name)
                    .withSecretBinary(data.binaryContent)
                    .withSecretString(data.stringContent)
                    .withKmsKeyId(data.kmsKeyId)
            )
        }
    }

    operator fun AWSSecretsManager.get(id: String): Any? {
        return try {
            val resp = getSecretValue(GetSecretValueRequest()
                    .withSecretId(id)
            )
            resp.secretString ?: resp.secretBinary
        } catch (e: ResourceNotFoundException) {
            null
        }
    }

    data class SecretData(val stringContent: String? = null, val binaryContent: ByteBuffer? = null, val kmsKeyId: String? = null)

    fun AWSSecretsManagerException.assertParamNotNullable(param: String) {
        assertThat(errorMessage).isEqualTo("1 validation error detected: Value null at '$param' failed to satisfy constraint: Member must not be null")
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun ResourceExistsException.assertIsCorrect(name: String) {
        assertThat(errorMessage).isEqualTo("The operation failed because the secret $name already exists.")
        assertThat(errorCode).isEqualTo("ResourceExistsException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun InvalidRequestException.cannotCreateDeletedSecret() {
        assertThat(errorMessage).isEqualTo("You can't create this secret because a secret with this name is already scheduled for deletion.")
        assertThat(errorCode).isEqualTo("InvalidRequestException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun InvalidRequestException.cannotUpdateDeletedSecret() {
        assertThat(errorMessage).isEqualTo("You can't perform this operation on the secret because it was marked for deletion.")
        assertThat(errorCode).isEqualTo("InvalidRequestException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun ResourceNotFoundException.assertIsCorrect() {
        assertThat(errorMessage).isEqualTo("Secrets Manager can't find the specified secret.")
        assertThat(errorCode).isEqualTo("ResourceNotFoundException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }

    fun InvalidParameterException.assertCantGiveBothTypes() {
        assertThat(errorMessage).isEqualTo("You can't specify both a binary secret value and a string secret value in the same secret.")
        assertThat(errorCode).isEqualTo("InvalidParameterException")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
    }
}