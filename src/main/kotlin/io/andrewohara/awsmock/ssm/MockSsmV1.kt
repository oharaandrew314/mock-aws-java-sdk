package io.andrewohara.awsmock.ssm

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simplesystemsmanagement.AbstractAWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.*
import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.ssm.backend.MockSsmBackend
import io.andrewohara.awsmock.ssm.backend.MockSsmParameter
import java.util.*

class MockSsmV1(private val backend: MockSsmBackend = MockSsmBackend()): AbstractAWSSimpleSystemsManagement() {

    override fun getParameter(request: GetParameterRequest): GetParameterResult {
        val param = try {
            backend.getParameter(request.name)
                .latest()
                .toParameter(request.name, request.withDecryption ?: false)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return GetParameterResult().withParameter(param)
    }

    override fun getParameters(request: GetParametersRequest): GetParametersResult {
        val params = backend.parameters()
            .filter { it.name in request.names }
            .map { it.latest().toParameter(it.name, request.withDecryption ?: false) }

        val invalid = request.names - params.map { it.name }

        return GetParametersResult()
                .withInvalidParameters(invalid)
                .withParameters(params)
    }

    override fun putParameter(request: PutParameterRequest): PutParameterResult {
        val parameter = try {
            backend.add(
                name = request.name,
                type = ParameterType.fromValue(request.type).toMock(),
                description = request.description,
                keyId = request.keyId,
                value = request.value,
                overwrite = request.overwrite ?: false
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return PutParameterResult().withVersion(parameter.latest().version)
    }

    override fun getParametersByPath(request: GetParametersByPathRequest): GetParametersByPathResult {
        val results = backend.parameters(prefix = request.path, limit = request.maxResults)
            .map { it.latest().toParameter(it.name,request.withDecryption ?: false) }

        return GetParametersByPathResult().withParameters(results)
    }

    override fun deleteParameter(request: DeleteParameterRequest): DeleteParameterResult {
        try {
            backend.delete(request.name)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return DeleteParameterResult()
    }

    override fun deleteParameters(request: DeleteParametersRequest): DeleteParametersResult {
        val deleted = request.names.mapNotNull { name ->
            try {
                backend.delete(name)
                name
            } catch (e: MockAwsException) {
                null
            }
        }

        return DeleteParametersResult()
                .withInvalidParameters(request.names - deleted)
                .withDeletedParameters(deleted)
    }

    override fun describeParameters(request: DescribeParametersRequest): DescribeParametersResult {
        val params = backend.parameters(limit = request.maxResults)
            .map { it.latest().toMetadata(it.name) }

        return DescribeParametersResult().withParameters(params)
    }

    override fun getParameterHistory(request: GetParameterHistoryRequest): GetParameterHistoryResult {
        val history = try {
            backend.getParameter(request.name).history()
                .map { it.toHistory(request.name, request.withDecryption) }
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return GetParameterHistoryResult().withParameters(history)
    }

    private fun MockSsmParameter.Value.toParameter(name: String, decrypt: Boolean?): Parameter = Parameter()
        .withName(name)
        .withType(type.toV1())
        .withValue(value(decrypt))
        .withVersion(version)

    private fun MockSsmParameter.Value.toMetadata(name: String): ParameterMetadata = ParameterMetadata()
        .withName(name)
        .withDescription(description)
        .withKeyId(keyId)
        .withType(type.toV1())
        .withVersion(version)

    private fun MockSsmParameter.Value.toHistory(name: String, decrypt: Boolean?): ParameterHistory = ParameterHistory()
        .withName(name)
        .withDescription(description)
        .withKeyId(keyId)
        .withType(type.toV1())
        .withVersion(version)
        .withValue(value(decrypt))

    private fun MockAwsException.toV1() = when(errorCode) {
        "ParameterAlreadyExists" -> ParameterAlreadyExistsException(message)
        "ParameterNotFound" -> ParameterNotFoundException(message)
        else -> AWSSimpleSystemsManagementException(message)
    }.also {
        it.requestId = UUID.randomUUID().toString()
        it.errorType = AmazonServiceException.ErrorType.Client
        it.errorCode = errorCode
        it.statusCode = 400
    }

    private fun MockSsmParameter.Type.toV1() = when(this) {
        MockSsmParameter.Type.StringList -> ParameterType.StringList
        MockSsmParameter.Type.Secure -> ParameterType.SecureString
        MockSsmParameter.Type.String -> ParameterType.String
    }

    private fun ParameterType.toMock() = when(this) {
        ParameterType.SecureString -> MockSsmParameter.Type.Secure
        ParameterType.String -> MockSsmParameter.Type.String
        ParameterType.StringList -> MockSsmParameter.Type.StringList
    }
}