package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import java.time.Instant
import java.util.*

class MockAmazonDynamoDB: AbstractAmazonDynamoDB() {

    private val tables = mutableSetOf<MockTable>()

    fun getTableOrNull(name: String) = tables.firstOrNull { name == it.name }
    fun getTable(name: String) = getTableOrNull(name) ?: throw createResourceNotFoundException()

    override fun createTable(request: CreateTableRequest): CreateTableResult {
        if (tables.any { it.name == request.tableName }) throw createResourceInUseException(request.tableName)

        val hashKeySchema = request.keySchema.first { it.keyType == KeyType.HASH.toString() }
        val hashKeyAttribute = request.attributeDefinitions.first { it.attributeName == hashKeySchema.attributeName }

        val rangeKeySchema = request.keySchema.firstOrNull { it.keyType == KeyType.RANGE.toString() }
        val rangeKeyAttribute = rangeKeySchema?.let {
            request.attributeDefinitions.firstOrNull { it.attributeName == rangeKeySchema.attributeName } ?: throw createValidationException()
        }

        val description = TableDescription().apply {
            tableName = request.tableName
            tableArn = UUID.randomUUID().toString()
            tableId = tableArn
            tableStatus = TableStatus.ACTIVE.toString()
            creationDateTime = Date.from(Instant.now())
            provisionedThroughput = request.provisionedThroughput.toDescription()

            setGlobalSecondaryIndexes((request.globalSecondaryIndexes ?: emptyList()).map { index ->
                GlobalSecondaryIndexDescription().apply {
                    indexArn = UUID.randomUUID().toString()
                    indexName = index.indexName
                    setKeySchema(index.keySchema)
                    projection = index.projection
                    provisionedThroughput = index.provisionedThroughput.toDescription()
                }
            })

            setLocalSecondaryIndexes((request.localSecondaryIndexes ?: emptyList()).map { index ->
                LocalSecondaryIndexDescription().apply {
                    indexArn = UUID.randomUUID().toString()
                    indexName = index.indexName
                    setKeySchema(index.keySchema)
                    projection = index.projection
                }
            })
        }

        val table = MockTable(description = description, hashKeyDef = hashKeyAttribute, rangeKeyDef = rangeKeyAttribute)
        tables.add(table)

        return CreateTableResult().withTableDescription(description)
    }

    override fun describeTable(request: DescribeTableRequest): DescribeTableResult {
        val table = getTable(request.tableName)
        return DescribeTableResult().withTable(table.description())
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
        for ((tableName, requestEntries) in request.requestItems) {
            val table = tables.firstOrNull { tableName == it.name } ?: throw createResourceNotFoundException()

            for (requestEntry in requestEntries) {
                requestEntry.deleteRequest?.let {
                    table.delete(it.key)
                }
                requestEntry.putRequest?.let {
                    table.save(it.item)
                }
            }
        }

        return BatchWriteItemResult().withUnprocessedItems(emptyMap())
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

        val items = table.query(
                keys = request.keyConditions ?: emptyMap(),
                filter = request.queryFilter ?: emptyMap(),
                scanIndexForward = request.isScanIndexForward ?: true
        )

        return QueryResult()
                .withCount(items.size)
                .withItems(items)
    }

    override fun batchGetItem(request: BatchGetItemRequest): BatchGetItemResult {
        val results: Map<String, MutableList<MockItem>> = request.requestItems
                .map { it.key to mutableListOf<MockItem>() }
                .toMap()

        for ((tableName, requestEntries) in request.requestItems) {
            val table = tables.firstOrNull { tableName == it.name } ?: throw createResourceNotFoundException()

            for (key in requestEntries.keys) {
                val item = table.get(key)
                if (item != null) {
                    results.getValue(tableName).add(item)
                }
            }
        }

        return BatchGetItemResult()
                .withResponses(results)
                .withUnprocessedKeys(emptyMap())
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

    private fun createValidationException() = AmazonDynamoDBException("One or more parameter values were invalid").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationException"
        statusCode = 400
    }
}

private fun ProvisionedThroughput.toDescription() = ProvisionedThroughputDescription()
        .withReadCapacityUnits(readCapacityUnits)
        .withWriteCapacityUnits(writeCapacityUnits)