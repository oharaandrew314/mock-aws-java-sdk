package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

class V2TableMapperUnitTest {

    private val backend = MockDynamoBackend()
    private val table = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(MockDynamoDbV2(backend))
        .build()
        .let { DynamoV2Cat.create(it) }

    private val toggles = DynamoV2Cat(2, "Toggles", "female")
    private val smokey = DynamoV2Cat(1, "Smokey", "female")
    private val bandit = DynamoV2Cat(1, "Bandit", "male")

    @Test
    fun `scan empty`() {
        table.scan().items().shouldBeEmpty()
    }

    @Test
    fun `scan all`() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.scan().items().toList().shouldContainExactlyInAnyOrder(bandit, smokey, toggles)
    }

    @Test
    fun `scan with EQ filter`() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.scan {
            it.filterExpression(
                Expression.builder()
                    .expression("gender = :gender")
                    .putExpressionValue(":gender", AttributeValue.builder().s("female").build())
                    .build()
            )
        }.items().toList().shouldContainExactlyInAnyOrder(smokey, toggles)
    }

    @Test
    fun `query empty table`() {
        table.query { builder: QueryEnhancedRequest.Builder ->
            builder.queryConditional(
                QueryConditional.keyEqualTo(toggles.toKey())
            )
        }.items().shouldBeEmpty()
    }

    @Test
    fun query() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.query { builder: QueryEnhancedRequest.Builder ->
            builder.queryConditional(
                QueryConditional.keyEqualTo(Key.builder().partitionValue(1).build())
            )
        }.items().toList().shouldContainExactly(bandit, smokey)
    }

    @Test
    fun `query and simple filter`() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.query { builder: QueryEnhancedRequest.Builder ->
            builder.queryConditional(
                QueryConditional.keyEqualTo(Key.builder().partitionValue(1).build())
            )
            builder.filterExpression(
                Expression.builder()
                    .expression("gender = :gender")
                    .putExpressionValue(":gender", AttributeValue.builder().s("female").build())
                    .build()
            )
        }.items().toList().shouldContainExactlyInAnyOrder(smokey)
    }

    @Test
    fun `query in reverse order`() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.query { builder: QueryEnhancedRequest.Builder ->
            builder.queryConditional(
                QueryConditional.keyEqualTo(Key.builder().partitionValue(1).build())
            )
            builder.scanIndexForward(false)
        }.items().toList().shouldContainExactly(smokey, bandit)
    }

    @Test
    fun `get missing`() {
        table.getItem(toggles.toKey()).shouldBeNull()
    }

    @Test
    fun get() {
        table.putItem(toggles)

        table.getItem(toggles.toKey()) shouldBe toggles
    }

    @Test
    fun `delete item`() {
        table.putItem(toggles)

        table.deleteItem(toggles)
        table.scan().items().shouldBeEmpty()
    }

    @Test
    fun `delete missing item`() {
        table.deleteItem(toggles)  // no error
    }

    @Test
    fun `delete table`() {
        table.deleteTable()

        backend.tables().shouldBeEmpty()
    }

    @Test
    fun `delete missing table`() {
        table.deleteTable()

        shouldThrow<ResourceNotFoundException> {
            table.deleteTable()
        }
    }

    @Test
    fun `query by global index`() {
        table.putItem(toggles)
        table.putItem(smokey)
        table.putItem(bandit)

        table.index("names").query(
            QueryConditional.keyEqualTo(Key.builder().partitionValue("Smokey").build())
        ).flatMap { it.items() }
            .toList()
            .shouldContainExactly(smokey)
    }
}

@DynamoDbBean
data class DynamoV2Cat(
    @get:DynamoDbPartitionKey var ownerId: Int? = null,

    @get:DynamoDbSortKey
    @get:DynamoDbSecondaryPartitionKey(indexNames = ["names"])
    var name: String? = null,

    var gender: String? = null,

    var features: List<String> = emptyList()
) {
    fun toKey(): Key = Key.builder().partitionValue(ownerId).sortValue(name).build()

    companion object {
        fun create(dynamo: DynamoDbEnhancedClient): DynamoDbTable<DynamoV2Cat> {
            val table = dynamo.table("cats", TableSchema.fromBean(DynamoV2Cat::class.java))

            table.createTable {
                it.globalSecondaryIndices(
                    EnhancedGlobalSecondaryIndex.builder().indexName("names").projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build(),
                )
            }

            return table
        }
    }
}