package io.andrewohara.awsmock.ssm

import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.simplesystemsmanagement.model.ParameterHistory
import com.amazonaws.services.simplesystemsmanagement.model.ParameterMetadata
import com.amazonaws.services.simplesystemsmanagement.model.ParameterType

data class MockParameter(
        val name: String,
        val type: ParameterType,
        val value: String,
        val keyId: String?,
        val version: Long,
        val description: String?
) {
    private fun secure() = type == ParameterType.SecureString

    fun toParameter(decrypt: Boolean): Parameter = Parameter()
            .withName(name)
            .withType(type)
            .withValue(if (secure() && !decrypt) "$keyId~$value" else value)
            .withVersion(version)

    fun toMetadata(): ParameterMetadata = ParameterMetadata()
            .withName(name)
            .withDescription(description)
            .withKeyId(keyId)
            .withType(type)
            .withVersion(version)

    fun toHistory(decrypt: Boolean): ParameterHistory = ParameterHistory()
            .withName(name)
            .withDescription(description)
            .withKeyId(keyId)
            .withType(type)
            .withVersion(version)
            .withValue(if (secure() && !decrypt) "$keyId~$value" else value)
}