package io.andrewohara.awsmock.ssm

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.*
import org.assertj.core.api.Assertions.*

object SsmUtils {

    operator fun AWSSimpleSystemsManagement.get(name: String): String? {
        val request = GetParameterRequest()
                .withName(name)

        return try {
            getParameter(request).parameter.value
        } catch (e: ParameterNotFoundException) {
            null
        }
    }

    operator fun AWSSimpleSystemsManagement.set(name: String, value: String) {
        set(name, Param(value = value))
    }

    operator fun AWSSimpleSystemsManagement.set(name: String, param: Param) {
        val request = PutParameterRequest()
                .withName(name)
                .withType(ParameterType.String)
                .withValue(param.value)
                .withDescription(param.description)
                .withOverwrite(true)

        putParameter(request)
    }

    operator fun AWSSimpleSystemsManagement.set(name: String, value: Collection<String>) {
        val request = PutParameterRequest()
                .withName(name)
                .withType(ParameterType.StringList)
                .withValue(value.joinToString(","))
                .withOverwrite(true)

        putParameter(request)
    }

    operator fun AWSSimpleSystemsManagement.set(name: String, param: SecureParam) {
        val request = PutParameterRequest()
                .withName(name)
                .withType(ParameterType.SecureString)
                .withValue(param.value)
                .withKeyId(param.keyId)
                .withDescription(param.description)
                .withOverwrite(true)

        putParameter(request)
    }

    fun AWSSimpleSystemsManagement.delete(name: String) {
        val request = DeleteParameterRequest()
                .withName(name)

        try {
            deleteParameter(request)
        } catch (e: ParameterNotFoundException) {
            // no-op
        }
    }

    fun ParameterNotFoundException.assertIsCorrect() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorCode).isEqualTo("ParameterNotFound")
        assertThat(errorMessage).isNull()
        assertThat(requestId).isNotEmpty()
    }

    fun ParameterAlreadyExistsException.assertIsCorrect() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorCode).isEqualTo("ParameterAlreadyExists")
        assertThat(errorMessage).isEqualTo("The parameter already exists. To overwrite this value, set the overwrite option in the request to true.")
        assertThat(requestId).isNotEmpty()
    }

    fun AWSSimpleSystemsManagementException.assertIsKeyIdNotRequired() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(errorMessage).isEqualTo("KeyId is required for SecureString type parameter only.")
        assertThat(requestId).isNotEmpty()
    }
}

data class SecureParam(val value: String, val description: String? = null, val keyId: String? = null)
data class Param(val value: String, val description: String? = null)