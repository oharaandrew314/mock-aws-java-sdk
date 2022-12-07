package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.time.Clock

private const val BATCH_GET_LIMIT = 100

class MockDynamoBackend(private val clock: Clock = Clock.systemUTC()) {

    private val tables = mutableListOf<MockDynamoTable>()
    fun tables(limit: Int? = null) = tables.take(limit ?: Int.MAX_VALUE).toList()

    operator fun get(name: TableName) = tables.find { it.schema.name == name }

    fun getAll(requests: List<TableAndItem>): BatchGetItemResult {
        val results = requests
            .take(BATCH_GET_LIMIT)
            .mapNotNull { (tableName, key) -> getTable(tableName)[key]?.let { TableAndItem(tableName, it) } }

        return BatchGetItemResult(
            results = results,
            unprocessed = requests.drop(BATCH_GET_LIMIT)
        )
    }

    fun getTable(name: TableName) = get(name) ?: throw resourceNotFound()

    fun deleteTable(name: TableName): MockDynamoTable {
        val table = get(name) ?: throw resourceNotFound()
        tables.remove(table)
        return table
    }

    fun createTable(
        name: TableName,
        hashKey: MockDynamoAttribute,
        rangeKey: MockDynamoAttribute? = null,
        globalIndices: Collection<MockDynamoSchema> = emptyList(),
        localIndices: Collection<MockDynamoSchema> = emptyList()
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