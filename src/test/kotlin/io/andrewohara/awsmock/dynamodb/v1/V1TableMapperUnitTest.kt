package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1TableMapperUnitTest {

    private val client = MockDynamoDbV1()
    private val mapper = DynamoDBMapper(
        client,
        DynamoDBMapperConfig.Builder()
            .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride("cats"))
            .build()
    ).newTableMapper<DynamoCat, Int, String>(DynamoCat::class.java)

    init {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    private val toggles = DynamoCat(2, "Toggles", "female")
    private val smokey = DynamoCat(1, "Smokey", "female")
    private val bandit = DynamoCat(1, "Bandit", "male")

    @Test
    fun `scan empty`() {
        mapper.scan(DynamoDBScanExpression()).shouldBeEmpty()
    }

    @Test
    fun `scan all`() {
        mapper.batchSave(setOf(toggles, smokey, bandit)).shouldBeEmpty()

        mapper.scan(DynamoDBScanExpression()).shouldContainExactlyInAnyOrder(bandit, smokey, toggles)
    }

    @Test
    fun `scan with EQ filter`() {
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBScanExpression()
            .withScanFilter(
                mapOf(
                    "gender" to Condition().withComparisonOperator(ComparisonOperator.EQ)
                        .withAttributeValueList(AttributeValue("female"))
                )
            )

        mapper.scan(expression).shouldContainExactlyInAnyOrder(smokey, toggles)
    }

    @Test
    fun `query empty table`() {
        val expression = DynamoDBQueryExpression<DynamoCat>()
            .withHashKeyValues(DynamoCat(1))

        mapper.query(expression).shouldBeEmpty()
    }

    @Test
    fun query() {
        mapper.batchSave(setOf(toggles, smokey, bandit)).shouldBeEmpty()

        val expression = DynamoDBQueryExpression<DynamoCat>()
            .withHashKeyValues(DynamoCat(1))

        mapper.query(expression).shouldContainExactly(bandit, smokey)
    }

    @Test
    fun `query in reverse order`() {
        mapper.batchSave(setOf(toggles, smokey, bandit)).shouldBeEmpty()

        val expression = DynamoDBQueryExpression<DynamoCat>()
            .withHashKeyValues(DynamoCat(1))
            .withScanIndexForward(false)

        mapper.query(expression).shouldContainExactly(smokey, bandit)
    }

    @Test
    fun `get missing`() {
        mapper.load(2, "Toggles").shouldBeNull()
    }

    @Test
    fun get() {
        mapper.save(toggles)

        mapper.load(2, "Toggles") shouldBe toggles
    }

    @Test
    fun `delete item`() {
        mapper.save(toggles)

        mapper.delete(toggles)
        mapper.scan(DynamoDBScanExpression()).shouldBeEmpty()
    }

    @Test
    fun `delete missing item`() {
        mapper.delete(toggles)  // no error
    }

    @Test
    fun `delete table`() {
        mapper.deleteTable()

        client.listTables().tableNames.shouldBeEmpty()
    }

    @Test
    fun `delete missing table`() {
        mapper.deleteTable()

        shouldThrow<ResourceNotFoundException> {
            mapper.deleteTable()
        }
    }
}

@DynamoDBTable(tableName = "cats")
data class DynamoCat(
    @DynamoDBHashKey
    var ownerId: Int? = null,

    @DynamoDBRangeKey
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "names")
    var name: String? = null,

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "genders")
    var gender: String? = null,

    var features: Set<String> = emptySet(),
    var visitDates: Set<Int> = emptySet()
)