package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException

data class MockDynamoSchema(
    val name: String,
    val hashKey: MockDynamoAttribute,
    val rangeKey: MockDynamoAttribute?
) {
    fun attributes() = listOfNotNull(hashKey, rangeKey)

    fun assertObeys(item: MockDynamoItem) {
        val hashValue = item[hashKey] ?: throw mismatchedKey(hashKey)
        if (hashKey.type != hashValue.type) throw mismatchedKey(hashKey, hashValue.type)

        if (rangeKey != null) {
            val rangeValue = item[rangeKey] ?: throw mismatchedKey(rangeKey)
            if (rangeKey.type != rangeValue.type) throw mismatchedKey(rangeKey, rangeValue.type)
        }
    }

    private fun mismatchedKey(key: MockDynamoAttribute) = MockAwsException(
        message = "One or more parameter values were invalid: Missing the key ${key.name} in the item",
        errorCode = "ValidationException",
        statusCode = 400
    )

    private fun mismatchedKey(key: MockDynamoAttribute, actual: MockDynamoAttribute.Type) = MockAwsException(
        message = "One or more parameter values were invalid: Type mismatch for key ${key.name} expected: ${key.type} actual: $actual",
        errorCode = "ValidationException",
        statusCode = 400
    )
}