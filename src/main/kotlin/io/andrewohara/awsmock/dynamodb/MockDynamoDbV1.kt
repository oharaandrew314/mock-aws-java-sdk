package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.dynamodb.backend.*
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.beginsWith
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.between
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.contains
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.eq
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.exists
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.ge
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.gt
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.inside
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.le
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition.Companion.lt
import java.util.*

class MockDynamoDbV1(private val backend: MockDynamoBackend = MockDynamoBackend()): AbstractAmazonDynamoDB() {

    override fun createTable(request: CreateTableRequest): CreateTableResult {
        val attributes = request.attributeDefinitions
            .map { it.toMock() }
            .associateBy { it.name }

        fun Collection<KeySchemaElement>.attribute(keyType: KeyType) = this
            .find { it.keyType == keyType.toString() }
            ?.let { attributes[it.attributeName] ?: throw validationFailed().toV1() }


        val globalIndices = request.globalSecondaryIndexes?.map { index ->
            MockDynamoSchema(
                name = index.indexName,
                hashKey = index.keySchema.attribute(KeyType.HASH) ?: throw validationFailed().toV1(),
                rangeKey = index.keySchema.attribute(KeyType.RANGE)
            )
        } ?: emptyList()

        val localIndices = request.localSecondaryIndexes?.map { index ->
            MockDynamoSchema(
                name = index.indexName,
                hashKey = index.keySchema.attribute(KeyType.HASH) ?: throw validationFailed().toV1(),
                rangeKey = index.keySchema.attribute(KeyType.RANGE)
            )
        } ?: emptyList()

        val table = try {
            backend.createTable(
                name = request.tableName,
                hashKey = request.keySchema.attribute(KeyType.HASH) ?: throw validationFailed().toV1(),
                rangeKey = request.keySchema.attribute(KeyType.RANGE),
                globalIndices = globalIndices,
                localIndices = localIndices
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return CreateTableResult().withTableDescription(table.toDescription())
    }

    override fun describeTable(request: DescribeTableRequest): DescribeTableResult {
        val table = try {
            backend.getTable(request.tableName)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return DescribeTableResult().withTable(table.toDescription())
    }

    override fun listTables(request: ListTablesRequest): ListTablesResult {
        val names = backend.tables(request.limit).map { it.schema.name }

        return ListTablesResult().withTableNames(names)
    }

    override fun putItem(request: PutItemRequest): PutItemResult {
        val item = try {
            val table = backend.getTable(request.tableName)
            val item = request.item.toMock()
            table.save(item)
            item
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return PutItemResult()
            .withAttributes(item.toV1())
    }

    override fun batchWriteItem(request: BatchWriteItemRequest): BatchWriteItemResult {
        try {
            val tablesAndEntries = request.requestItems.map { (tableName, entries) ->
                backend.getTable(tableName) to entries
            }

            for ((table, requestEntries) in tablesAndEntries) {
                for (requestEntry in requestEntries) {
                    requestEntry.deleteRequest?.let {
                        table.delete(it.key.toMock())
                    }
                    requestEntry.putRequest?.let {
                        table.save(it.item.toMock())
                    }
                }
            }
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return BatchWriteItemResult().withUnprocessedItems(emptyMap())
    }

    override fun updateItem(request: UpdateItemRequest): UpdateItemResult {
        val item = try {
            val table = backend.getTable(request.tableName)
            table.update(
                key = request.key.toMock(),
                updates = request.attributeUpdates.mapValues { it.value.toMock() }
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return UpdateItemResult().apply {
            if (item != null) {
                withAttributes(item.toV1())
            }
        }
    }

    override fun getItem(request: GetItemRequest): GetItemResult {
        val item = try {
            val table = backend.getTable(request.tableName)
            table[request.key.toMock()]
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return GetItemResult().withItem(item?.toV1())
    }

    override fun deleteItem(request: DeleteItemRequest): DeleteItemResult {
        val item = try {
            val table = backend.getTable(request.tableName)
            table.delete(request.key.toMock())
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return DeleteItemResult().apply {
            if (item != null) {
                withAttributes(item.toV1())
            }
        }
    }

    override fun scan(request: ScanRequest): ScanResult {
        val items = try {
            val table = backend.getTable(request.tableName)
            val conditions = request.scanFilter.toMock() +  MockDynamoCondition.parseExpression(
                expression = request.filterExpression ?: "",
                values = request.expressionAttributeValues.toMock()
            )

            table.scan(conditions)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return ScanResult()
            .withCount(items.size)
            .withItems(items.map { it.toV1() })
    }

    override fun query(request: QueryRequest): QueryResult {
        val conditions = (request.keyConditions.toMock() + request.queryFilter.toMock()).toMutableMap()
        conditions += MockDynamoCondition.parseExpression(
            expression = request.filterExpression ?: "",
            values = request.expressionAttributeValues.toMock()
        )

        val items = try {
            val table = backend.getTable(request.tableName)

            table.query(
                conditions = conditions,
                scanIndexForward = request.scanIndexForward ?: true,
                indexName = request.indexName
            )
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return QueryResult()
            .withCount(items.size)
            .withItems(items.map { it.toV1() })
    }

    override fun batchGetItem(request: BatchGetItemRequest): BatchGetItemResult {
        val requests = request.requestItems
            .mapValues { it.value.keys.map { key -> key.toMock() } }

        val results = try {
            backend.getAll(requests)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return BatchGetItemResult()
            .withResponses(results.mapValues { it.value.map { item -> item.toV1() } })
            .withUnprocessedKeys(emptyMap())  // TODO implement
    }

    override fun deleteTable(request: DeleteTableRequest): DeleteTableResult {
        val table = try {
            backend.deleteTable(request.tableName)
        } catch (e: MockAwsException) {
            throw e.toV1()
        }

        return DeleteTableResult()
            .withTableDescription(table.toDescription())
    }

    companion object {
        private fun AttributeDefinition.toMock() = MockDynamoAttribute(
            name = attributeName,
            type = when(ScalarAttributeType.valueOf(attributeType)) {
                ScalarAttributeType.B -> MockDynamoAttribute.Type.Binary
                ScalarAttributeType.N -> MockDynamoAttribute.Type.Number
                ScalarAttributeType.S -> MockDynamoAttribute.Type.String
            }
        )
        fun MockDynamoAttribute.toV1() = AttributeDefinition()
            .withAttributeName(name)
            .withAttributeType(when(type) {
                MockDynamoAttribute.Type.Binary -> ScalarAttributeType.B
                MockDynamoAttribute.Type.Number -> ScalarAttributeType.N
                MockDynamoAttribute.Type.String -> ScalarAttributeType.S
                else -> throw validationFailed().toV1()
            })

        private fun MockDynamoSchema.toV1KeySchema() = listOfNotNull(
            KeySchemaElement().withKeyType(KeyType.HASH).withAttributeName(hashKey.name),
            rangeKey?.let { KeySchemaElement().withKeyType(KeyType.RANGE).withAttributeName(it.name) }
        )
        private fun MockAwsException.toV1() = when(errorCode) {
            "ResourceNotFoundException" -> ResourceNotFoundException(message)
            "ResourceInUseException" -> ResourceInUseException(message)
            else -> AmazonDynamoDBException(message)
        }.also {
            it.requestId = UUID.randomUUID().toString()
            it.errorType = AmazonServiceException.ErrorType.Client
            it.errorCode = errorCode
            it.statusCode = statusCode
        }

        private fun MockDynamoTable.toDescription() = TableDescription()
            .withTableArn(arn)
            .withTableName(schema.name)
            .withAttributeDefinitions(attributes().map { it.toV1() })
            .withKeySchema(schema.toV1KeySchema())
            .withCreationDateTime(Date.from(created))
            .withTableStatus(TableStatus.ACTIVE)
            .withItemCount(items.size.toLong())
            .withGlobalSecondaryIndexes(
                globalIndices.map {
                    GlobalSecondaryIndexDescription()
                        .withIndexName(it.name)
                        .withIndexStatus(IndexStatus.ACTIVE)
                        .withItemCount(items.size.toLong())
                        .withKeySchema(it.toV1KeySchema())
                        .withIndexArn("$arn/index/${it.name}")
                }
            )
            .withLocalSecondaryIndexes(
                localIndices.map {
                    LocalSecondaryIndexDescription()
                        .withIndexName(it.name)
                        .withItemCount(items.size.toLong())
                        .withKeySchema(it.toV1KeySchema())
                        .withIndexArn("$arn/index/${it.name}")
                }
            )

        fun MockDynamoItem.toV1(): Map<String, AttributeValue> = attributes
            .map { (attr, value) -> attr to value.toV1() }
            .toMap()

        fun MockDynamoValue.toV1(): AttributeValue = AttributeValue()
            .withS(s)
            .withN(n?.toPlainString())
            .withB(b)
            .withBOOL(bool)
            .withSS(ss)
            .withNS(ns?.map { it.toPlainString() })
            .withBS(bs)
            .withL(list?.map { it.toV1() })
            .withM(map?.toV1())

        private fun AttributeValue.toMock(): MockDynamoValue = MockDynamoValue(
            s = s,
            n = n?.toBigDecimal(),
            b = b,
            bool = bool,
            ss = ss?.toSet(),
            ns = ns?.map(String::toBigDecimal)?.toSet(),
            bs = bs?.toSet(),
            list = l?.map { it.toMock() },
            map = m?.mapValues { it.value.toMock() }?.let { MockDynamoItem(it) }
        )

        private fun Condition.toMock(): MockDynamoCondition {
            fun arg(index: Int = 0) = attributeValueList[index]?.toMock() ?: throw validationFailed()

            return when(ComparisonOperator.valueOf(comparisonOperator)) {
                ComparisonOperator.EQ -> eq(arg())
                ComparisonOperator.NE -> eq(arg())
                ComparisonOperator.LT -> lt(arg())
                ComparisonOperator.LE -> le(arg())
                ComparisonOperator.GT -> gt(arg())
                ComparisonOperator.GE -> ge(arg())
                ComparisonOperator.CONTAINS -> contains(arg())
                ComparisonOperator.NOT_CONTAINS -> contains(arg()).not()
                ComparisonOperator.NULL -> !exists()
                ComparisonOperator.NOT_NULL -> exists()
                ComparisonOperator.BEGINS_WITH -> beginsWith(arg())
                ComparisonOperator.BETWEEN -> between(arg(0), arg(1))
                ComparisonOperator.IN -> inside(attributeValueList.map { it.toMock() })
            }
        }

        private fun Map<String, Condition>?.toMock() = this?.mapValues { it.value.toMock() } ?: emptyMap()

        private fun Map<String, AttributeValue>?.toMock() = MockDynamoItem(
            attributes = this?.mapValues { it.value.toMock() } ?: emptyMap()
        )

        private fun AttributeValueUpdate.toMock() = MockDynamoUpdate(
            action = when (AttributeAction.valueOf(action)) {
                AttributeAction.ADD -> MockDynamoUpdate.Type.Add
                AttributeAction.DELETE -> MockDynamoUpdate.Type.Delete
                AttributeAction.PUT -> MockDynamoUpdate.Type.Put
            },
            value = value?.toMock()
        )
    }
}