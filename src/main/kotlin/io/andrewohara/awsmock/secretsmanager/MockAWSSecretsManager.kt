package io.andrewohara.awsmock.secretsmanager

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.secretsmanager.AbstractAWSSecretsManager
import com.amazonaws.services.secretsmanager.model.*
import java.util.*

class MockAWSSecretsManager: AbstractAWSSecretsManager() {

    private val secrets = mutableSetOf<MockSecret>()

    private fun get(id: String) = secrets
            .firstOrNull { it.arn == id || it.name == id }

    override fun createSecret(request: CreateSecretRequest): CreateSecretResult {
        if (request.name == null) {
            throw AWSSecretsManagerException("1 validation error detected: Value null at 'name' failed to satisfy constraint: Member must not be null").apply {
                requestId = UUID.randomUUID().toString()
                errorType = AmazonServiceException.ErrorType.Client
                errorCode = "ValidationException"
                statusCode = 400
            }
        }

        val existing = get(request.name)
        if (existing != null && !existing.deleted) {
            throw ResourceExistsException("The operation failed because the secret ${request.name} already exists.").apply {
                requestId = UUID.randomUUID().toString()
                errorType = AmazonServiceException.ErrorType.Client
                errorCode = "ResourceExistsException"
                statusCode = 400
            }
        } else if (existing != null && existing.deleted) {
            throw InvalidRequestException("You can't create this secret because a secret with this name is already scheduled for deletion.").apply {
                requestId = UUID.randomUUID().toString()
                errorType = AmazonServiceException.ErrorType.Client
                errorCode = "InvalidRequestException"
                statusCode = 400
            }
        }

        val secret = MockSecret(
                name = request.name,
                description = request.description,
                keyId = request.kmsKeyId,
                contentString = request.secretString,
                contentBinary = request.secretBinary,
                tags = request.tags
        )

        secrets.add(secret)

        return CreateSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersionId(secret.latest().version)
    }

    override fun describeSecret(request: DescribeSecretRequest): DescribeSecretResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()

        val versionIdsToStages = mutableMapOf(secret.latest().version to listOf("AWSCURRENT"))
        secret.previous()?.let { previous ->
            versionIdsToStages[previous.version] = listOf("AWSPREVIOUS")
        }

        return DescribeSecretResult()
                .withARN(secret.arn)
                .withDescription(secret.description)
                .withKmsKeyId(secret.kmsKeyId)
                .withName(secret.name)
                .withTags(secret.tags)
                .withVersionIdsToStages(versionIdsToStages)
    }

    override fun deleteSecret(request: DeleteSecretRequest): DeleteSecretResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()
        secret.deleted = true

        return DeleteSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
    }

    override fun getSecretValue(request: GetSecretValueRequest): GetSecretValueResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()
        val latest = secret.latest()

        return GetSecretValueResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withSecretBinary(latest.binary())
                .withSecretString(latest.string())
                .withVersionId(latest.version)
                .withVersionStages(listOf("AWSCURRENT"))
    }

    override fun updateSecret(request: UpdateSecretRequest): UpdateSecretResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()
        if (secret.deleted) {
            throw createCannotUpdateDeletedSecret()
        }

        secret.update(
                string = request.secretString,
                binary = request.secretBinary,
                description = request.description,
                kmsKeyId = request.kmsKeyId
        )

        return UpdateSecretResult()
                .withARN(secret.arn)
                .withName(secret.name)
    }

    override fun listSecrets(request: ListSecretsRequest): ListSecretsResult {
        val entries = secrets
                .take(request.maxResults ?: Int.MAX_VALUE)
                .map { secret ->
                    SecretListEntry()
                            .withARN(secret.arn)
                            .withName(secret.name)
                            .withKmsKeyId(secret.kmsKeyId)
                            .withTags(secret.tags)
                }

        return ListSecretsResult().withSecretList(entries)
    }

    override fun putSecretValue(request: PutSecretValueRequest): PutSecretValueResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()
        if (secret.deleted) {
            throw createCannotUpdateDeletedSecret()
        }

        val latest = secret.insert(request.secretBinary, request.secretString)


        return PutSecretValueResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersionId(latest.version)
                .withVersionStages(listOf("AWSCURRENT"))
    }

    override fun listSecretVersionIds(request: ListSecretVersionIdsRequest): ListSecretVersionIdsResult {
        val secret = get(request.secretId) ?: throw createNotFoundException()

        return ListSecretVersionIdsResult()
                .withARN(secret.arn)
                .withName(secret.name)
                .withVersions(secret.secretVersionListEntries())
    }

    private fun createNotFoundException() = ResourceNotFoundException("Secrets Manager can't find the specified secret.").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ResourceNotFoundException"
        statusCode = 400
    }

    private fun createCannotUpdateDeletedSecret() = InvalidRequestException("You can't perform this operation on the secret because it was marked for deletion.").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "InvalidRequestException"
        statusCode = 400
    }
}