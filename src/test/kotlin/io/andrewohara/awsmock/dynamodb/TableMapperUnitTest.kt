package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.fixtures.DynamoCat
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class TableMapperUnitTest {

    private val client = MockAmazonDynamoDB()
    private val mapper = CatsFixtures.mapper(client).also { mapper ->
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    private val toggles = DynamoCat(2, "Toggles", "female")
    private val smokey = DynamoCat(1, "Smokey", "female")
    private val bandit = DynamoCat(1, "Bandit", "male")

    @Test
    fun `scan empty`() {
        val results = mapper.scan(DynamoDBScanExpression())
        assertThat(results).isEmpty()
    }

    @Test
    fun `scan all`() {
        mapper.batchSave(setOf(toggles, smokey, bandit))

        assertThat(mapper.scan(DynamoDBScanExpression())).containsExactlyInAnyOrder(bandit, smokey, toggles)
    }

    @Test
    fun `scan with EQ filter`() {
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBScanExpression()
                .withScanFilter(mapOf("gender" to Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(AttributeValue("female"))))

        assertThat(mapper.scan(expression)).containsExactlyInAnyOrder(smokey, toggles)
    }

    @Test
    fun `query empty table`() {
        val expression = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(1))

        assertThat(mapper.query(expression)).isEmpty()
    }

    @Test
    fun query() {
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(1))

        assertThat(mapper.query(expression)).containsExactly(bandit, smokey)
    }

    @Test
    fun `query in reverse order`() {
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(1))
                .withScanIndexForward(false)

        assertThat(mapper.query(expression)).containsExactly(smokey, bandit)
    }

    @Test
    fun `get missing`() {
        val item = mapper.load(2, "Toggles")
        assertThat(item).isNull()
    }

    @Test
    fun get() {
        mapper.save(toggles)

        assertThat(mapper.load(2, "Toggles")).isEqualTo(toggles)
    }

    @Test
    fun `delete item`() {
        mapper.save(toggles)

        mapper.delete(toggles)
        assertThat(mapper.scan(DynamoDBScanExpression())).isEmpty()
    }

    @Test
    fun `delete missing item`() {
        mapper.delete(toggles)  // no error
    }

    @Test
    fun `delete table`() {
        mapper.deleteTable()

        assertThat(client.listTables().tableNames).isEmpty()
    }

    @Test
    fun `delete missing table`() {
        mapper.deleteTable()

        val exception = catchThrowableOfType({ mapper.deleteTable() }, ResourceNotFoundException::class.java)
        exception.assertIsNotFound()
    }
}