package io.andrewohara.awsmock.dynamodb

import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.dynamodb.backend.*
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.*

class MockDynamoDbV2(private val backend: MockDynamoBackend = MockDynamoBackend()): DynamoDbClient {
    override fun close() {}
    override fun serviceName() = "dynamodb-mock"

    override fun createTable(request: CreateTableRequest): CreateTableResponse {
        val attributes = request.attributeDefinitions()
            .map { it.toMock() }
            .associateBy { it.name }

        fun Collection<KeySchemaElement>.attribute(keyType: KeyType) = this
            .find { it.keyType() == keyType }
            ?.let { attributes[it.attributeName()] ?: throw validationFailed().toV2() }


        val globalIndices = request.globalSecondaryIndexes()?.map { index ->
            MockDynamoSchema(
                name = index.indexName(),
                hashKey = index.keySchema().attribute(KeyType.HASH) ?: throw validationFailed().toV2(),
                rangeKey = index.keySchema().attribute(KeyType.RANGE)
            )
        } ?: emptyList()

        val localIndices = request.localSecondaryIndexes()?.map { index ->
            MockDynamoSchema(
                name = index.indexName(),
                hashKey = index.keySchema().attribute(KeyType.HASH) ?: throw validationFailed().toV2(),
                rangeKey = index.keySchema().attribute(KeyType.RANGE)
            )
        } ?: emptyList()

        val table = try {
            backend.createTable(
                name = request.tableName(),
                hashKey = request.keySchema().attribute(KeyType.HASH) ?: throw validationFailed().toV2(),
                rangeKey = request.keySchema().attribute(KeyType.RANGE),
                globalIndices = globalIndices,
                localIndices = localIndices
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return CreateTableResponse.builder()
            .tableDescription(table.toDescription())
            .build()
    }

    override fun batchGetItem(request: BatchGetItemRequest): BatchGetItemResponse {
        val requests = request.requestItems()
            .mapValues { it.value.keys().map { key -> key.toMock() } }

        val results = try {
            backend.getAll(requests)
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return BatchGetItemResponse.builder()
            .responses(results.mapValues { it.value.map { item -> item.toV2() } })
            .unprocessedKeys(emptyMap())  // TODO implement
            .build()
    }

    override fun batchWriteItem(request: BatchWriteItemRequest): BatchWriteItemResponse {
        try {
            val tablesAndEntries = request.requestItems().map { (tableName, entries) ->
                backend.getTable(tableName) to entries
            }

            for ((table, requestEntries) in tablesAndEntries) {
                for (requestEntry in requestEntries) {
                    requestEntry.deleteRequest()?.let {
                        table.delete(it.key().toMock())
                    }
                    requestEntry.putRequest()?.let {
                        table.save(it.item().toMock())
                    }
                }
            }
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return BatchWriteItemResponse.builder()
            .unprocessedItems(emptyMap())
            .build()
    }

    override fun deleteItem(request: DeleteItemRequest): DeleteItemResponse {
        val item = try {
            val table = backend.getTable(request.tableName())
            table.delete(request.key().toMock())
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return DeleteItemResponse.builder()
            .attributes(item?.toV2())
            .build()
    }

    override fun getItem(request: GetItemRequest): GetItemResponse {
        val item = try {
            val table = backend.getTable(request.tableName())
            table[request.key().toMock()]
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return GetItemResponse.builder()
            .item(item?.toV2())
            .build()
    }

    override fun deleteTable(request: DeleteTableRequest): DeleteTableResponse {
        val table = try {
            backend.deleteTable(request.tableName())
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return DeleteTableResponse.builder()
            .tableDescription(table.toDescription())
            .build()
    }

    override fun describeTable(request: DescribeTableRequest): DescribeTableResponse {
        val table = try {
            backend.getTable(request.tableName())
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return DescribeTableResponse.builder()
            .table(table.toDescription())
            .build()
    }

    override fun listTables(request: ListTablesRequest): ListTablesResponse {
        val names = backend.tables(request.limit()).map { it.schema.name }

        return ListTablesResponse.builder()
            .tableNames(names)
            .build()
    }

    override fun putItem(request: PutItemRequest): PutItemResponse {
        val item = try {
            val table = backend.getTable(request.tableName())
            val item = request.item().toMock()
            table.save(item)
            item
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return PutItemResponse.builder()
            .attributes(item.toV2())
            .build()
    }

    fun MockAwsException.toV2() = when(errorCode) {
        "ResourceNotFoundException" -> ResourceNotFoundException.builder()
        "ResourceInUseException" -> ResourceInUseException.builder()
        else -> DynamoDbException.builder()
    }.requestId(UUID.randomUUID().toString())
        .statusCode(statusCode)
        .message(message)
        .awsErrorDetails(
            AwsErrorDetails.builder()
                .errorMessage(message)
                .serviceName(serviceName())
                .errorCode(errorCode)
                .build()
        )
        .build()

    override fun query(request: QueryRequest): QueryResponse {
        val conditions = (request.keyConditions() ?: emptyMap()) + (request.queryFilter() ?: emptyMap())

        val items = try {
            val table = backend.getTable(request.tableName())

            table.query(
                conditions = conditions.map { it.toMock() },
                scanIndexForward = request.scanIndexForward() ?: true,
                indexName = request.indexName()
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return QueryResponse.builder()
            .count(items.size)
            .items(items.map { it.toV2() })
            .build()
    }

    override fun scan(request: ScanRequest): ScanResponse {
        val items = try {
            val table = backend.getTable(request.tableName())

            table.scan(
                conditions = request.scanFilter()?.map { it.toMock() } ?: emptyList()
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return ScanResponse.builder()
            .count(items.size)
            .items(items.map { it.toV2() })
            .build()
    }

    override fun updateItem(request: UpdateItemRequest): UpdateItemResponse {
        val item = try {
            val table = backend.getTable(request.tableName())
            table.update(
                key = request.key().toMock(),
                updates = request.attributeUpdates().mapValues { it.value.toMock() }
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return UpdateItemResponse.builder()
            .attributes(item?.toV2())
            .build()
    }

    companion object {
        fun MockDynamoAttribute.toV2(): AttributeDefinition = AttributeDefinition.builder()
            .attributeName(name)
            .attributeType(when(type) {
                MockDynamoAttribute.Type.Binary -> ScalarAttributeType.B
                MockDynamoAttribute.Type.Number -> ScalarAttributeType.N
                MockDynamoAttribute.Type.String -> ScalarAttributeType.S
                else -> null
            })
            .build()

        fun MockDynamoItem.toV2(): Map<String, AttributeValue> = attributes
            .map { (attr, value) -> attr to value.toV2() }
            .toMap()

        fun MockDynamoValue.toV2(): AttributeValue = AttributeValue.builder()
            .s(s)
            .n(n?.toPlainString())
            .b(b?.let(SdkBytes::fromByteBuffer))
            .bool(bool)
            .ss(ss)
            .ns(ns?.map { it.toPlainString() })
            .bs(bs?.map { SdkBytes.fromByteBuffer(it) })
            .l(list?.map { it.toV2() })
            .m(map?.toV2())
            .build()

        private fun AttributeValue.toMock(): MockDynamoValue = MockDynamoValue(
            s = s(),
            n = n()?.toBigDecimal(),
            b = b()?.asByteBuffer(),
            bool = bool(),
            ss = ss()
                ?.toSet()
                ?.takeIf { hasSs() }
            ,
            ns = ns()
                ?.map(String::toBigDecimal)
                ?.toSet()
                ?.takeIf { hasNs() }
            ,
            bs = bs()
                ?.map { it.asByteBuffer() }
                ?.toSet()
                ?.takeIf { hasBs() }
            ,
            list = l()
                ?.map { it.toMock() }
                ?.takeIf { hasL() }
            ,
            map = m()
                ?.mapValues { it.value.toMock() }
                ?.takeIf { hasM() }
                ?.let { MockDynamoItem(it) }
        )

        private fun Map<String, AttributeValue>.toMock() = MockDynamoItem(
            attributes = mapValues { it.value.toMock() }.toMutableMap()
        )

        private fun AttributeDefinition.toMock() = MockDynamoAttribute(
            name = attributeName(),
            type = when(attributeType()) {
                ScalarAttributeType.B -> MockDynamoAttribute.Type.Binary
                ScalarAttributeType.N -> MockDynamoAttribute.Type.Number
                ScalarAttributeType.S -> MockDynamoAttribute.Type.String
                null, ScalarAttributeType.UNKNOWN_TO_SDK_VERSION -> throw UnsupportedOperationException()
            }
        )

        fun MockDynamoSchema.toV1KeySchema() = listOfNotNull(
            KeySchemaElement.builder()
                .keyType(KeyType.HASH)
                .attributeName(hashKey.name)
                .build(),
            rangeKey?.let {
                KeySchemaElement.builder()
                    .keyType(KeyType.RANGE)
                    .attributeName(it.name)
                    .build()
            }
        )

        private fun MockDynamoTable.toDescription() = TableDescription.builder()
            .tableArn(arn)
            .tableName(schema.name)
            .attributeDefinitions(attributes().map { it.toV2() })
            .keySchema(schema.toV1KeySchema())
            .creationDateTime(created)
            .tableStatus(TableStatus.ACTIVE)
            .itemCount(items.size.toLong())
            .globalSecondaryIndexes(
                globalIndices.map {
                    GlobalSecondaryIndexDescription.builder()
                        .indexName(it.name)
                        .indexStatus(IndexStatus.ACTIVE)
                        .itemCount(items.size.toLong())
                        .keySchema(it.toV1KeySchema())
                        .indexArn("$arn/index/${it.name}")
                        .build()
                }
            )
            .localSecondaryIndexes(
                localIndices.map {
                    LocalSecondaryIndexDescription.builder()
                        .indexName(it.name)
                        .itemCount(items.size.toLong())
                        .keySchema(it.toV1KeySchema())
                        .indexArn("$arn/index/${it.name}")
                        .build()
                }
            )
            .build()

        private fun Map.Entry<String, Condition>.toMock(): ItemCondition {
            val (name, condition) = this

            fun arg(index: Int = 0) = condition.attributeValueList()[index]?.toMock() ?: throw validationFailed()

            return when(condition.comparisonOperator()) {
                ComparisonOperator.EQ -> Conditions.eq(arg()).forAttribute(name)
                ComparisonOperator.NE -> Conditions.eq(arg()).forAttribute(name)
                ComparisonOperator.LT -> Conditions.lt(arg()).forAttribute(name)
                ComparisonOperator.LE -> Conditions.le(arg()).forAttribute(name)
                ComparisonOperator.GT -> Conditions.gt(arg()).forAttribute(name)
                ComparisonOperator.GE -> Conditions.ge(arg()).forAttribute(name)
                ComparisonOperator.CONTAINS -> Conditions.contains(arg()).forAttribute(name)
                ComparisonOperator.NOT_CONTAINS -> Conditions.contains(arg()).not().forAttribute(name)
                ComparisonOperator.NULL -> Conditions.exists(name).inv()
                ComparisonOperator.NOT_NULL -> Conditions.exists(name)
                ComparisonOperator.BEGINS_WITH -> Conditions.beginsWith(arg()).forAttribute(name)
                ComparisonOperator.BETWEEN -> Conditions.between(arg(0)..arg(1)).forAttribute(name)
                ComparisonOperator.IN -> Conditions.inside(condition.attributeValueList().map { it.toMock() }).forAttribute(name)
                ComparisonOperator.UNKNOWN_TO_SDK_VERSION, null -> throw IllegalStateException()
            }
        }

        private fun AttributeValueUpdate.toMock() = MockDynamoUpdate(
            action = when (action()) {
                AttributeAction.ADD -> MockDynamoUpdate.Type.Add
                AttributeAction.DELETE -> MockDynamoUpdate.Type.Delete
                AttributeAction.PUT -> MockDynamoUpdate.Type.Put
                AttributeAction.UNKNOWN_TO_SDK_VERSION, null -> throw IllegalStateException()
            },
            value = value()?.toMock()
        )
    }
}