package io.andrewohara.awsmock.secretsmanager

import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.secretsmanager.backend.MockSecretsBackend
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.*
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException
import software.amazon.awssdk.services.secretsmanager.model.ListSecretVersionIdsRequest
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import software.amazon.awssdk.services.secretsmanager.model.Tag
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest
import java.util.*

class MockSecretsManagerV2(private val backend: MockSecretsBackend = MockSecretsBackend()): SecretsManagerClient {

    override fun close() {}
    override fun serviceName() = "secrets-mock"

    override fun createSecret(request: CreateSecretRequest): CreateSecretResponse {
        val (secret, version) = try {
            backend.create(
                name = request.name(),
                description = request.description(),
                tags = request.tags()?.associate { it.key() to it.value() },
                kmsKeyId = request.kmsKeyId(),
                secretString = request.secretString(),
                secretBinary = request.secretBinary()?.asByteBuffer()
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return CreateSecretResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .versionId(version?.versionId)
            .build()
    }

    override fun describeSecret(request: DescribeSecretRequest): DescribeSecretResponse {
        val secret = getOrThrow(request.secretId())
        val versions = secret.versions().reversed().associate { version ->
            version.versionId to version.stages
        }

        return DescribeSecretResponse.builder()
            .arn(secret.arn)
            .description(secret.description)
            .kmsKeyId(secret.kmsKeyId)
            .name(secret.name)
            .tags(secret.tags?.map { it.toTag() })
            .versionIdsToStages(versions)
            .build()
    }

    override fun deleteSecret(request: DeleteSecretRequest): DeleteSecretResponse {
        val secret = try {
            backend.deleteSecret(request.secretId())
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return DeleteSecretResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .build()
    }

    override fun getSecretValue(request: GetSecretValueRequest): GetSecretValueResponse {
        val secret = getOrThrow(request.secretId())
        val latest = secret.latest()

        return GetSecretValueResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .secretBinary(latest?.binary?.let(SdkBytes::fromByteBuffer))
            .secretString(latest?.string)
            .versionId(latest?.versionId)
            .versionStages(latest?.stages)
            .build()
    }

    override fun updateSecret(request: UpdateSecretRequest): UpdateSecretResponse {
        val (secret, version) = try {
            backend.updateSecret(
                secretId = request.secretId(),
                description = request.description(),
                secretString = request.secretString(),
                secretBinary = request.secretBinary()?.asByteBuffer(),
                kmsKeyId = request.kmsKeyId()
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return UpdateSecretResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .versionId(version?.versionId)
            .build()
    }

    override fun listSecrets(request: ListSecretsRequest): ListSecretsResponse {
        val entries = backend.secrets(request.maxResults()).map { secret ->
            SecretListEntry.builder()
                .arn(secret.arn)
                .name(secret.name)
                .kmsKeyId(secret.kmsKeyId)
                .description(secret.description)
                .tags(secret.tags?.map { it.toTag() })
                .build()
        }

        return ListSecretsResponse.builder()
            .secretList(entries)
            .build()
    }

    override fun putSecretValue(request: PutSecretValueRequest): PutSecretValueResponse {
        val (secret, version) = try {
            backend.putSecretValue(
                secretId = request.secretId(),
                secretString = request.secretString(),
                secretBinary = request.secretBinary()?.asByteBuffer()
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return PutSecretValueResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .versionId(version?.versionId)
            .versionStages(version?.stages)
            .build()
    }

    override fun listSecretVersionIds(request: ListSecretVersionIdsRequest): ListSecretVersionIdsResponse {
        val secret = getOrThrow(request.secretId())

        val versions = secret.versions().reversed().map { version ->
            SecretVersionsListEntry.builder()
                .versionId(version.versionId)
                .versionStages(version.stages)
                .build()
        }

        return ListSecretVersionIdsResponse.builder()
            .arn(secret.arn)
            .name(secret.name)
            .versions(versions)
            .build()
    }

    private fun Map.Entry<String, String>.toTag() = Tag.builder().key(key).value(value).build()

    private fun getOrThrow(id: String) = try {
        backend.describeSecret(id)
    } catch (e: MockAwsException) {
        throw e.toV2()
    }

    private fun MockAwsException.toV2() = when(errorCode) {
        "InvalidParameterException" -> InvalidParameterException.builder()
        "ResourceNotFoundException" -> ResourceNotFoundException.builder()
        "ValidationException" -> SecretsManagerException.builder()
        "ResourceExistsException" -> ResourceExistsException.builder()
        "InvalidRequestException" -> InvalidRequestException.builder()
        else -> AwsServiceException.builder()
    }
        .message(message)
        .requestId(UUID.randomUUID().toString())
        .statusCode(statusCode)
        .awsErrorDetails(
            AwsErrorDetails.builder()
                .serviceName(serviceName())
                .errorCode(errorCode)
                .errorMessage(message)
                .build()
        )
        .build()
}