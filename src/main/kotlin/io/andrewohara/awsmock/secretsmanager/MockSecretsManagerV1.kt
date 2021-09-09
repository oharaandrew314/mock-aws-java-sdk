package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.secretsmanager.AbstractAWSSecretsManager
import com.amazonaws.services.secretsmanager.model.*
import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import java.util.*

class MockSecretsManagerV1(
    private val backend: MockSecretsBackend = MockSecretsBackend()
) : AbstractAWSSecretsManager() {

    override fun createSecret(request: CreateSecretRequest): CreateSecretResult {
        val (secret, version) = try {
            backend.create(
                name = request.name,
                description = request.description,
                tags = request.tags?.associate { it.key to it.value },
                kmsKeyId = request.kmsKeyId,
                secretString = request.secretString,
                secretBinary = request.secretBinary,
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return CreateSecretResult()
            .withARN(secret.arn)
            .withName(secret.name)
            .withVersionId(version?.versionId)
    }

    override fun describeSecret(request: DescribeSecretRequest): DescribeSecretResult {
        val secret = getOrThrow(request.secretId)
        val versions = secret.versions().reversed().associate { version ->
            version.versionId to version.stages
        }

        return DescribeSecretResult()
            .withARN(secret.arn)
            .withDescription(secret.description)
            .withKmsKeyId(secret.kmsKeyId)
            .withName(secret.name)
            .withTags(secret.tags?.map { it.toTag() })
            .withVersionIdsToStages(versions)
    }

    override fun deleteSecret(request: DeleteSecretRequest): DeleteSecretResult {
        val secret = try {
            backend.deleteSecret(request.secretId)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return DeleteSecretResult()
            .withARN(secret.arn)
            .withName(secret.name)
    }

    override fun getSecretValue(request: GetSecretValueRequest): GetSecretValueResult {
        val secret = getOrThrow(request.secretId)
        val latest = secret.latest()

        return GetSecretValueResult()
            .withARN(secret.arn)
            .withName(secret.name)
            .withSecretBinary(latest?.binary)
            .withSecretString(latest?.string)
            .withVersionId(latest?.versionId)
            .withVersionStages(latest?.stages)
    }

    override fun updateSecret(request: UpdateSecretRequest): UpdateSecretResult {
        val (secret, version) = try {
            backend.updateSecret(
                secretId = request.secretId,
                description = request.description,
                secretString = request.secretString,
                secretBinary = request.secretBinary,
                kmsKeyId = request.kmsKeyId
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return UpdateSecretResult()
            .withARN(secret.arn)
            .withName(secret.name)
            .withVersionId(version?.versionId)
    }

    override fun listSecrets(request: ListSecretsRequest): ListSecretsResult {
        val entries = backend.secrets(request.maxResults).map { secret ->
            SecretListEntry()
                .withARN(secret.arn)
                .withName(secret.name)
                .withKmsKeyId(secret.kmsKeyId)
                .withTags(secret.tags?.map { it.toTag() })
        }

        return ListSecretsResult().withSecretList(entries)
    }

    override fun putSecretValue(request: PutSecretValueRequest): PutSecretValueResult {
        val (secret, version) = try {
            backend.putSecretValue(
                secretId = request.secretId,
                secretString = request.secretString,
                secretBinary = request.secretBinary
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return PutSecretValueResult()
            .withARN(secret.arn)
            .withName(secret.name)
            .withVersionId(version?.versionId)
            .withVersionStages(version?.stages)
    }

    override fun listSecretVersionIds(request: ListSecretVersionIdsRequest): ListSecretVersionIdsResult {
        val secret = getOrThrow(request.secretId)

        val versions = secret.versions().reversed().map { version ->
            SecretVersionsListEntry().withVersionId(version.versionId).withVersionStages(version.stages)
        }

        return ListSecretVersionIdsResult()
            .withARN(secret.arn)
            .withName(secret.name)
            .withVersions(versions)
    }

    private fun Map.Entry<String, String>.toTag() = Tag().withKey(key).withValue(value)

    private fun getOrThrow(id: String) = try {
        backend.describeSecret(id)
    } catch (e: MockAwsException) {
        throw e.toV1()
    }

    private fun MockAwsException.toV1() = when (errorCode) {
        "InvalidParameterException" -> InvalidParameterException(message)
        "ResourceNotFoundException" -> ResourceNotFoundException(message)
        "ValidationException" -> AWSSecretsManagerException(message)
        "ResourceExistsException" -> ResourceExistsException(message)
        "InvalidRequestException" -> InvalidRequestException(message)
        else -> AmazonServiceException(message)
    }.also { v1 ->
        v1.requestId = UUID.randomUUID().toString()
        v1.errorType = AmazonServiceException.ErrorType.Client
        v1.errorCode = errorCode
        v1.statusCode = statusCode
    }
}