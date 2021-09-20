package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.time.Clock

class MockDynamoBackend(private val clock: Clock = Clock.systemUTC()) {

    private val tables = mutableListOf<MockDynamoTable>()
    fun tables(limit: Int? = null) = tables.take(limit ?: Int.MAX_VALUE).toList()

    operator fun get(name: String) = tables.find { it.schema.name == name }

    fun getAll(requests: Map<String, Collection<MockDynamoItem>>): Map<String, List<MockDynamoItem>> {
        val results = mutableListOf<Pair<String, MockDynamoItem>>()

        for ((tableName, keys) in requests) {
            val table = getTable(tableName)

            results += keys
                .mapNotNull { table[it] }
                .map { tableName to it }
        }

        return results.groupBy({ it.first }, { it.second })
    }

    fun getTable(name: String) = get(name) ?: throw resourceNotFound()

    fun deleteTable(name: String) {
        val removed = tables.removeIf { it.schema.name == name }
        if (!removed) throw resourceNotFound()
    }

    fun createTable(
        name: String,
        hashKey: MockDynamoAttribute,
        rangeKey: MockDynamoAttribute?,
        globalIndices: Collection<MockDynamoSchema>,
        localIndices: Collection<MockDynamoSchema>
    ): MockDynamoTable {
        get(name)?.let { throw resourceInUse(name) }

        val table = MockDynamoTable(
            schema = MockDynamoSchema(
                name = name,
                hashKey = hashKey,
                rangeKey = rangeKey
            ),
            created = clock.instant(),
            globalIndices = globalIndices,
            localIndices = localIndices
        )

        tables.add(table)

        return table
    }

    private fun resourceNotFound() = MockAwsException(
        errorCode = "ResourceNotFoundException",
        statusCode = 400,
        message = "Requested resource not found"
    )

    private fun resourceInUse(tableName: String) = MockAwsException(
        message = "Table already exists: $tableName",
        errorCode = "ResourceInUseException",
        statusCode = 400
    )
}