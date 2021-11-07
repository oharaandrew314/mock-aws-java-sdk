package io.andrewohara.awsmock.dynamodb.backend


object PeopleTable {

    fun create(backend: MockDynamoBackend): MockDynamoTable {
        return backend.createTable(
            name = "indexTable",
            hashKey = MockDynamoAttribute(MockDynamoAttribute.Type.Number, "id"),
            globalIndices = listOf(
                MockDynamoSchema(
                    name = "names",
                    hashKey = MockDynamoAttribute(MockDynamoAttribute.Type.String, "lastName"),
                    rangeKey = MockDynamoAttribute(MockDynamoAttribute.Type.String, "firstName")
                )
            )
        )
    }

    val johnDoe = MockDynamoItem(
        "id" to MockDynamoValue(1),
        "firstName" to MockDynamoValue("John"),
        "lastName" to MockDynamoValue("Doe")
    )
    val janeDoe = MockDynamoItem(
        "id" to MockDynamoValue(2),
        "firstName" to MockDynamoValue("Jane"),
        "lastName" to MockDynamoValue("Doe")
    )
    val billSmith = MockDynamoItem(
        "id" to MockDynamoValue(3),
        "firstName" to MockDynamoValue("Bill"),
        "lastName" to MockDynamoValue("Smith")
    )
}