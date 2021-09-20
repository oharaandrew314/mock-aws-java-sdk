package io.andrewohara.awsmock.dynamodb.backend

data class MockDynamoAttribute(
    val type: Type,
    val name: String
) {
    enum class Type {
        String, Number, Binary,
        StringSet, NumberSet, BinarySet,
        Boolean, List, Map, Null
    }
}