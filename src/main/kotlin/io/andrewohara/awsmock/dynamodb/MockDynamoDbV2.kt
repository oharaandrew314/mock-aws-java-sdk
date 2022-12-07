package io.andrewohara.awsmock.dynamodb

import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.dynamodb.backend.BatchGetItemResult
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoAttribute
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoCondition
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoSchema
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoUpdate
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.andrewohara.awsmock.dynamodb.backend.TableAndItem
import io.andrewohara.awsmock.dynamodb.backend.validationFailed
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.ApiName
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.util.VersionInfo
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeAction
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator
import software.amazon.awssdk.services.dynamodb.model.Condition
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription
import software.amazon.awssdk.services.dynamodb.model.IndexStatus
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndexDescription
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.ScanResponse
import software.amazon.awssdk.services.dynamodb.model.TableDescription
import software.amazon.awssdk.services.dynamodb.model.TableStatus
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse
import software.amazon.awssdk.services.dynamodb.paginators.BatchGetItemIterable
import software.amazon.awssdk.services.dynamodb.paginators.QueryIterable
import software.amazon.awssdk.services.dynamodb.paginators.ScanIterable
import java.util.UUID
import java.util.function.Consumer


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
        return try {
            val result = backend.getAll(request.toInternal())
            if (request.isPaginated()) result else result.throwIfUnprocessed()
        } catch (e: MockAwsException) {
            throw e.toV2()
        }.toV2()
    }

    private fun BatchGetItemRequest.toInternal() = requestItems().flatMap { (tableName, data) ->
        data.keys().map { TableAndItem(tableName, it.toMock()) }
    }

    private fun BatchGetItemResult.toV2() = BatchGetItemResponse.builder()
        .responses(results.groupBy({it.tableName}, {it.item.toV2()}))
        .unprocessedKeys(
            unprocessed
                .groupBy { it.tableName }
                .mapValues { (_, keys) -> KeysAndAttributes.builder().keys(keys.map { it.item.toV2() }).build() }
        )
        .build()

    override fun batchGetItemPaginator(request: BatchGetItemRequest): BatchGetItemIterable {
        return BatchGetItemIterable(this, request.withPagination())
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

    override fun query(request: QueryRequest): QueryResponse {
        val conditions = request.keyConditions().toMock().toMutableMap()
        conditions += MockDynamoCondition.parseExpression(
            expression = request.keyConditionExpression() ?: "",
            values = request.expressionAttributeValues().toMock()
        )
        conditions += MockDynamoCondition.parseExpression(
            expression = request.filterExpression() ?: "",
            values = request.expressionAttributeValues().toMock()
        )

        val items = try {
            val table = backend.getTable(request.tableName())

            table.query(
                conditions = conditions,
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

    override fun queryPaginator(request: QueryRequest) = QueryIterable(this, request)

    override fun scan(request: ScanRequest): ScanResponse {
        val items = try {
            val table = backend.getTable(request.tableName())

            val conditions = request.scanFilter().toMock().toMutableMap()
            conditions += MockDynamoCondition.parseExpression(
                expression = request.filterExpression() ?: "",
                values = request.expressionAttributeValues().toMock()
            )

            table.scan(conditions)
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return ScanResponse.builder()
            .count(items.size)
            .items(items.map { it.toV2() })
            .build()
    }

    override fun updateItem(request: UpdateItemRequest): UpdateItemResponse {
        // TODO support update expression
        val item = try {
            val table = backend.getTable(request.tableName())
            table.update(
                key = request.key().toMock(),
                updates = (request.attributeUpdates() ?: emptyMap()).mapValues { it.value.toMock() }
            )
        } catch (e: MockAwsException) {
            throw e.toV2()
        }

        return UpdateItemResponse.builder()
            .attributes(item?.toV2())
            .build()
    }

    override fun scanPaginator(request: ScanRequest): ScanIterable {
        return ScanIterable(this, request)
    }

    fun MockAwsException.toV2(): AwsServiceException = when(errorCode) {
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

        fun MockDynamoValue.toV2(): AttributeValue = AttributeValue.builder().let { builder ->
            when(type) {
                MockDynamoAttribute.Type.Binary -> builder.b(b?.let(SdkBytes::fromByteBuffer))
                MockDynamoAttribute.Type.Boolean -> builder.bool(bool)
                MockDynamoAttribute.Type.List -> builder.l(list?.map { it.toV2() })
                MockDynamoAttribute.Type.Map -> builder.m(map?.toV2())
                MockDynamoAttribute.Type.Null -> builder.nul(true)
                MockDynamoAttribute.Type.Number -> builder.n(n?.toPlainString())
                MockDynamoAttribute.Type.BinarySet -> builder.bs(bs?.map { SdkBytes.fromByteBuffer(it) })
                MockDynamoAttribute.Type.NumberSet -> builder.ns(ns?.map { it.toPlainString() })
                MockDynamoAttribute.Type.String -> builder.s(s)
                MockDynamoAttribute.Type.StringSet -> builder.ss(ss)
            }
        }.build()

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

        private fun Map<String, AttributeValue>?.toMock() = MockDynamoItem(
            attributes = this?.mapValues { it.value.toMock() } ?: emptyMap()
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

        private fun MockDynamoSchema.toV1KeySchema() = listOfNotNull(
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

        private fun Map<String, Condition>?.toMock() = this?.mapValues { it.value.toMock() } ?: emptyMap()

        private fun Condition.toMock(): MockDynamoCondition {
            fun arg(index: Int = 0) = attributeValueList()[index]?.toMock() ?: throw validationFailed()

            return when(comparisonOperator()) {
                ComparisonOperator.EQ -> MockDynamoCondition.eq(arg())
                ComparisonOperator.NE -> MockDynamoCondition.eq(arg())
                ComparisonOperator.LT -> MockDynamoCondition.lt(arg())
                ComparisonOperator.LE -> MockDynamoCondition.le(arg())
                ComparisonOperator.GT -> MockDynamoCondition.gt(arg())
                ComparisonOperator.GE -> MockDynamoCondition.ge(arg())
                ComparisonOperator.CONTAINS -> MockDynamoCondition.contains(arg())
                ComparisonOperator.NOT_CONTAINS -> MockDynamoCondition.contains(arg()).not()
                ComparisonOperator.NULL -> !MockDynamoCondition.exists()
                ComparisonOperator.NOT_NULL -> MockDynamoCondition.exists()
                ComparisonOperator.BEGINS_WITH -> MockDynamoCondition.beginsWith(arg())
                ComparisonOperator.BETWEEN -> MockDynamoCondition.between(arg(0), arg(1))
                ComparisonOperator.IN -> MockDynamoCondition.inside(attributeValueList().map { it.toMock() })
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

    private fun <T : DynamoDbRequest> DynamoDbRequest.withPagination(): T {
        val userAgentApplier = Consumer<AwsRequestOverrideConfiguration.Builder> {
            val apiName = ApiName.builder()
                .version(VersionInfo.SDK_VERSION).name("PAGINATED")
                .build()
            it.addApiName(apiName)
        }
        val overrideConfiguration = overrideConfiguration()
            .map { it.toBuilder().applyMutation(userAgentApplier).build() }
            .orElse(AwsRequestOverrideConfiguration.builder().applyMutation(userAgentApplier).build())

        return toBuilder().overrideConfiguration(overrideConfiguration).build() as T
    }
}

private fun DynamoDbRequest.isPaginated() = overrideConfiguration()
    .map { config -> config.apiNames().any { it.name() == "PAGINATED" } }
    .orElse(false)
