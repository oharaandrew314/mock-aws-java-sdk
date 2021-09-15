package io.andrewohara.awsmock.ssm.backend

import io.andrewohara.awsmock.core.MockAwsException

class MockSsmBackend {

    private val parameters = mutableListOf<MockSsmParameter>()
    fun parameters(prefix: String? = null, limit: Int? = null) = parameters
        .filter { if (prefix == null) true else it.name.startsWith(prefix) }
        .take(limit ?: Int.MAX_VALUE)
        .toList()

    operator fun get(name: String) = parameters.find { it.name == name }
    operator fun set(name: String, value: String) = add(name, MockSsmParameter.Type.String, value, overwrite = true)
    operator fun set(name: String, values: List<String>) = add(name, MockSsmParameter.Type.StringList, values.joinToString(","), overwrite = true)
    fun secure(name: String, value: String, keyId: String? = null) = add(name, MockSsmParameter.Type.Secure, value, keyId = keyId, overwrite = true)

    fun getParameter(name: String) = get(name) ?: throw paramNotFound(name)

    fun add(name: String, type: MockSsmParameter.Type, value: String, description: String? = null, keyId: String? = null, overwrite: Boolean = false): MockSsmParameter {
        val existing = get(name)

        if (existing != null) {
            if (!overwrite) throw MockAwsException(400, "ParameterAlreadyExists", "The parameter already exists. To overwrite this value, set the overwrite option in the request to true.")

            existing.add(type = type, value = value, description = description, keyId = keyId)
            return existing
        }

        val parameter = MockSsmParameter(
            name = name,
            type = type,
            value = value,
            description = description,
            keyId = keyId
        )
        parameters += parameter

        return parameter
    }

    fun getHistory(name: String): List<MockSsmParameter.Value> {
        val param = get(name) ?: throw paramNotFound(name)
        return param.history()
    }

    fun delete(name: String) {
        val deleted = parameters.removeIf { it.name == name }
        if (!deleted) throw paramNotFound(name)
    }

    private fun paramNotFound(name: String) = MockAwsException(400, "ParameterNotFound", "Parameter not found: $name")
}