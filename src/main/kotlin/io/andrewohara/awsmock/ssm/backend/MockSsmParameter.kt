package io.andrewohara.awsmock.ssm.backend

import io.andrewohara.awsmock.core.MockAwsException

class MockSsmParameter(
    val name: String,
    type: Type,
    value: String,
    keyId: String?,
    description: String?
) {
    private val history = mutableListOf<Value>()
    fun history() = history.toList()

    init {
        add(type, value, keyId, description)
    }

    fun latest() = history.last()
    fun add(type: Type, value: String, keyId: String? = null, description: String? = null) {
        history += Value(
            type = type,
            value = value,
            keyId = keyId ?: if (type.secure()) "defaultKey" else null,
            version = history.size + 1L,
            description = description
        )
    }

    enum class Type { Secure, String, StringList;
        fun secure() = this == Secure
    }

    data class Value(
        val type: Type,
        private val value: String,
        val keyId: String?,
        val version: Long,
        val description: String?
    ) {
        init {
            if (!type.secure() && keyId != null) {
                throw MockAwsException(400, "ValidationException", "KeyId is required for SecureString type parameter only.")
            }
        }

        fun value(decrypt: Boolean?) = if (!type.secure() || decrypt == true) value else "$keyId~$value"
    }
}