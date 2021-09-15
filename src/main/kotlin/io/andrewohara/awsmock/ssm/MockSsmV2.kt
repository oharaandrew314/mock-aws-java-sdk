package io.andrewohara.awsmock.ssm

import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.*
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest
import software.amazon.awssdk.services.ssm.model.DeleteParametersRequest
import software.amazon.awssdk.services.ssm.model.DescribeParametersRequest
import software.amazon.awssdk.services.ssm.model.GetParameterHistoryRequest
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest
import software.amazon.awssdk.services.ssm.model.GetParametersRequest
import software.amazon.awssdk.services.ssm.model.Parameter
import software.amazon.awssdk.services.ssm.model.ParameterAlreadyExistsException
import software.amazon.awssdk.services.ssm.model.ParameterHistory
import software.amazon.awssdk.services.ssm.model.ParameterMetadata
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import java.lang.IllegalArgumentException
import java.util.*

class MockSsmV2(private val backend: MockSsmBackend = MockSsmBackend()): SsmClient {
    override fun close() {}
    override fun serviceName() = "ssm-mock"

    override fun getParameter(request: GetParameterRequest): GetParameterResponse {
        val param = try {
            backend.getParameter(request.name())
                .toParameter(request.withDecryption() ?: false)
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return GetParameterResponse.builder()
            .parameter(param)
            .build()
    }

    override fun getParameters(request: GetParametersRequest): GetParametersResponse {
        val params = backend.parameters()
            .filter { it.name in request.names() }
            .map { it.toParameter(request.withDecryption() ?: false) }

        val invalid = request.names() - params.map { it.name() }

        return GetParametersResponse.builder()
            .invalidParameters(invalid)
            .parameters(params)
            .build()
    }

    override fun putParameter(request: PutParameterRequest): PutParameterResponse {
        val parameter = try {
            backend.add(
                name = request.name(),
                type = request.type().toMock(),
                description = request.description(),
                keyId = request.keyId(),
                value = request.value(),
                overwrite = request.overwrite() ?: false
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return PutParameterResponse.builder()
            .version(parameter.latest().version)
            .build()
    }

    override fun getParametersByPath(request: GetParametersByPathRequest): GetParametersByPathResponse {
        val results = backend.parameters(prefix = request.path(), limit = request.maxResults())
            .map { it.toParameter(request.withDecryption() ?: false) }

        return GetParametersByPathResponse.builder()
            .parameters(results)
            .build()
    }

    override fun deleteParameter(request: DeleteParameterRequest): DeleteParameterResponse {
        try {
            backend.delete(request.name())
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return DeleteParameterResponse.builder()
            .build()
    }

    override fun deleteParameters(request: DeleteParametersRequest): DeleteParametersResponse {
        val deleted = request.names().mapNotNull { name ->
            try {
                backend.delete(name)
                name
            } catch (e: MockAwsException) {
                null
            }
        }

        return DeleteParametersResponse.builder()
            .invalidParameters(request.names() - deleted)
            .deletedParameters(deleted)
            .build()
    }

    override fun describeParameters(request: DescribeParametersRequest): DescribeParametersResponse {
        val params = backend.parameters(limit = request.maxResults())
            .map { it.latest().toMetadata(it.name) }

        return DescribeParametersResponse.builder()
            .parameters(params)
            .build()
    }

    override fun getParameterHistory(request: GetParameterHistoryRequest): GetParameterHistoryResponse {
        val history = try {
            backend.getParameter(request.name()).history()
                .map { it.toHistory(request.name(), request.withDecryption()) }
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return GetParameterHistoryResponse.builder()
            .parameters(history)
            .build()
    }

    private fun MockSsmParameter.toParameter(decrypt: Boolean?): Parameter {
        val latest = latest()
        return Parameter.builder()
            .name(name)
            .type(latest.type.toV2())
            .value(latest.value(decrypt))
            .version(latest.version)
            .arn(arn())
            .build()
    }

    private fun MockSsmParameter.Value.toMetadata(name: String): ParameterMetadata = ParameterMetadata.builder()
        .name(name)
        .description(description)
        .keyId(keyId)
        .type(type.toV2())
        .version(version)
        .build()

    private fun MockSsmParameter.Value.toHistory(name: String, decrypt: Boolean?): ParameterHistory = ParameterHistory.builder()
        .name(name)
        .description(description)
        .keyId(keyId)
        .type(type.toV2())
        .version(version)
        .value(value(decrypt))
        .build()

    private fun MockAwsException.toV2() = when(errorCode) {
        "ParameterAlreadyExists" -> ParameterAlreadyExistsException.builder()
        "ParameterNotFound" -> ParameterNotFoundException.builder()
        else -> SsmException.builder()
    }
        .requestId(UUID.randomUUID().toString())
        .statusCode(statusCode)
        .awsErrorDetails(
            AwsErrorDetails.builder()
                .errorCode(errorCode)
                .errorMessage(message)
                .serviceName(serviceName())
                .build()
        )
        .build()

    private fun MockSsmParameter.Type.toV2() = when(this) {
        MockSsmParameter.Type.StringList -> ParameterType.STRING_LIST
        MockSsmParameter.Type.Secure -> ParameterType.SECURE_STRING
        MockSsmParameter.Type.String -> ParameterType.STRING
    }

    private fun ParameterType.toMock() = when(this) {
        ParameterType.SECURE_STRING -> MockSsmParameter.Type.Secure
        ParameterType.STRING -> MockSsmParameter.Type.String
        ParameterType.STRING_LIST -> MockSsmParameter.Type.StringList
        ParameterType.UNKNOWN_TO_SDK_VERSION -> throw IllegalArgumentException("Unsupported parameter type: $this")
    }

    private fun MockSsmParameter.arn() = "arn:aws:${serviceName()}:ca-central-1:111222333444:parameter/${name.trimStart('/')}"
}