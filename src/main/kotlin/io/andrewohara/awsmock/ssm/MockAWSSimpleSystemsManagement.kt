package io.andrewohara.awsmock.ssm

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simplesystemsmanagement.AbstractAWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.*
import java.util.*

class MockAWSSimpleSystemsManagement: AbstractAWSSimpleSystemsManagement() {

    private val params = mutableMapOf<String, MutableList<MockParameter>>()

    override fun getParameter(request: GetParameterRequest): GetParameterResult {
        val param = params[request.name]
                ?.last()
                ?.toParameter(request.withDecryption ?: false)
                ?: throw createParameterNotFound()

        return GetParameterResult().withParameter(param)
    }

    override fun getParameters(request: GetParametersRequest): GetParametersResult {
        val invalid = mutableListOf<String>()
        val result = mutableListOf<Parameter>()

        for (name in request.names) {
            val param = params[name]?.last()?.toParameter(request.withDecryption == true)
            if (param == null) {
                invalid.add(name)
            } else {
                result.add(param)
            }
        }

        return GetParametersResult()
                .withInvalidParameters(invalid)
                .withParameters(result)
    }

    override fun putParameter(request: PutParameterRequest): PutParameterResult {
        val secure = ParameterType.fromValue(request.type) == ParameterType.SecureString

        if (!secure && request.keyId != null) {
            throw AWSSimpleSystemsManagementException("KeyId is required for SecureString type parameter only.").apply {
                requestId = UUID.randomUUID().toString()
                errorType = AmazonServiceException.ErrorType.Client
                errorCode = "ValidationException"
                statusCode = 400
            }
        }

        if (params.containsKey(request.name) && request.isOverwrite != true) {
            throw ParameterAlreadyExistsException("The parameter already exists. To overwrite this value, set the overwrite option in the request to true.").apply {
                requestId = UUID.randomUUID().toString()
                errorType = AmazonServiceException.ErrorType.Client
                errorCode = "ParameterAlreadyExists"
                statusCode = 400
            }
        }

        val history = params[request.name] ?: mutableListOf()

        val parameter = MockParameter(
                name = request.name,
                type = ParameterType.fromValue(request.type),
                value = request.value,
                description = request.description,
                version = history.size.toLong() + 1,
                keyId = if (secure) request.keyId ?: "defaultKey" else null
        )
        history.add(parameter)

        params[request.name] = history

        return PutParameterResult().withVersion(parameter.version)
    }

    override fun getParametersByPath(request: GetParametersByPathRequest): GetParametersByPathResult {
        val results = params
                .filterKeys { it.startsWith(request.path) }
                .map { it.value.last().toParameter(request.withDecryption == true) }
                .take(request.maxResults ?: Int.MAX_VALUE)

        return GetParametersByPathResult().withParameters(results)
    }

    override fun deleteParameter(request: DeleteParameterRequest): DeleteParameterResult {
        params.remove(request.name) ?: throw createParameterNotFound()

        return DeleteParameterResult()
    }

    override fun deleteParameters(request: DeleteParametersRequest): DeleteParametersResult {
        val invalid = mutableListOf<String>()
        val result = mutableListOf<String>()

        for (name in request.names) {
            if (params.containsKey(name)) {
                params.remove(name)
                result.add(name)
            } else {
                invalid.add(name)
            }
        }

        return DeleteParametersResult()
                .withInvalidParameters(invalid)
                .withDeletedParameters(result)
    }

    override fun describeParameters(request: DescribeParametersRequest): DescribeParametersResult {
        val metadata = params.values
                .map { it.last().toMetadata() }
                .take(request.maxResults ?: Int.MAX_VALUE)

        return DescribeParametersResult().withParameters(metadata)
    }

    override fun getParameterHistory(request: GetParameterHistoryRequest): GetParameterHistoryResult {
        val history = params[request.name]
                ?.take(request.maxResults ?: Int.MAX_VALUE)
                ?.map { it.toHistory(request.withDecryption == true) }
                ?: throw createParameterNotFound()

        return GetParameterHistoryResult().withParameters(history)
    }

    private fun createParameterNotFound() = ParameterNotFoundException(null).apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ParameterNotFound"
        statusCode = 400
    }
}