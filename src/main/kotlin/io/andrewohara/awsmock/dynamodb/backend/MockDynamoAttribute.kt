package io.andrewohara.awsmock.dynamodb.backend

data class MockDynamoAttribute(
    val type: Type,
    val name: String
) {
    companion object {
        fun string(name: String) = MockDynamoAttribute(Type.String, name)
        fun number(name: String) = MockDynamoAttribute(Type.Number, name)
    }

    enum class Type {
        String, Number, Binary,
        StringSet, NumberSet, BinarySet,
        Boolean, List, Map, Null
    }
}