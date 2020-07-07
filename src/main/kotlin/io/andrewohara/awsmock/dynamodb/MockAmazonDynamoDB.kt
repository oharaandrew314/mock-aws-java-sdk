package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import java.util.*

class MockAmazonDynamoDB: AbstractAmazonDynamoDB() {

    private val tables = mutableSetOf<MockTable>()

    private fun getTable(name: String) = tables.firstOrNull { name == it.name } ?: throw createResourceNotFoundException()

    override fun createTable(request: CreateTableRequest): CreateTableResult {
        if (tables.any { it.name == request.tableName }) throw createResourceInUseException(request.tableName)

        val table = MockTable(
                name = request.tableName,
                hashKeyDef = request.keySchema.first(),
                hashKeyType = ScalarAttributeType.fromValue(request.attributeDefinitions.first().attributeType),
                rangeKeyDef = request.keySchema.lastOrNull(),
                rangeKeyType = ScalarAttributeType.fromValue(request.attributeDefinitions.last().attributeType)
        )

        tables.add(table)

        return CreateTableResult()
    }

    override fun listTables(request: ListTablesRequest): ListTablesResult {
        val names =  tables
                .take(request.limit ?: Int.MAX_VALUE)
                .map { it.name }

        return ListTablesResult().withTableNames(names)
    }

    override fun putItem(request: PutItemRequest): PutItemResult {
        val table = getTable(request.tableName)

        table.save(request.item)

        return PutItemResult()
    }

    override fun batchWriteItem(request: BatchWriteItemRequest): BatchWriteItemResult {
        val unprocessed = mutableMapOf<String, List<WriteRequest>>()

        for ((tableName, requestEntries) in request.requestItems) {
            val table = tables.firstOrNull { tableName == it.name }
            if (table == null) {
                unprocessed[tableName] = requestEntries
                continue
            }

            for (requestEntry in requestEntries) {
                requestEntry.deleteRequest?.let {
                    table.delete(it.key)
                }
                requestEntry.putRequest?.let {
                    table.save(it.item)
                }
            }
        }

        return BatchWriteItemResult().withUnprocessedItems(unprocessed)
    }

    override fun updateItem(request: UpdateItemRequest): UpdateItemResult {
        val table = getTable(request.tableName)

        val item = (table.get(request.key) ?: request.key).toMutableMap()
        val updated = item.update(request.attributeUpdates)
        table.save(updated)

        return UpdateItemResult()
    }

    override fun getItem(request: GetItemRequest): GetItemResult {
        val table = getTable(request.tableName)

        val item = table.get(request.key)

        return GetItemResult().withItem(item)
    }

    override fun deleteItem(request: DeleteItemRequest): DeleteItemResult {
        val table = getTable(request.tableName)

        table.delete(request.key)

        return DeleteItemResult()
    }

    override fun scan(request: ScanRequest): ScanResult {
        val table = getTable(request.tableName)

        val items = table.scan(request.scanFilter)

        return ScanResult()
                .withCount(items.size)
                .withItems(items)
    }

    override fun query(request: QueryRequest): QueryResult {
        val table = getTable(request.tableName)

        val items = table.query(request.keyConditions, request.queryFilter, request.isScanIndexForward ?: true)

        return QueryResult()
                .withCount(items.size)
                .withItems(items)
    }

    override fun deleteTable(request: DeleteTableRequest): DeleteTableResult {
        val table = getTable(request.tableName)
        tables.remove(table)

        return DeleteTableResult()
    }

    private fun createResourceNotFoundException() = ResourceNotFoundException("Requested resource not found").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ResourceNotFoundException"
        statusCode = 400
    }

    private fun createResourceInUseException(tableName: String) = ResourceInUseException("Table already exists: $tableName").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ResourceInUseException"
        statusCode = 400
    }
}