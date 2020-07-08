package io.andrewohara.awsmock.dynamodb.mapper

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.DynamoCat
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class TableMapperUnitTest {

    private val client = MockAmazonDynamoDB()
    private val mapper = DynamoCat.mapper(client)

    @Before
    fun setup() {
        mapper.createTable(ProvisionedThroughput(1, 1))
    }

    @Test
    fun `scan empty`() {
        val results = mapper.scan(DynamoDBScanExpression())
        assertThat(results).isEmpty()
    }

    @Test
    fun `scan all`() {
        val toggles = DynamoCat(1, "Toggles", "female")
        val smokey = DynamoCat(2, "Smokey", "female")
        val bandit = DynamoCat(2, "Bandit", "male")
        mapper.batchSave(setOf(toggles, smokey, bandit))

        assertThat(mapper.scan(DynamoDBScanExpression())).containsExactlyInAnyOrder(bandit, smokey, toggles)
    }

    @Test
    fun `scan with EQ filter`() {
        val toggles = DynamoCat(1, "Toggles", "female")
        val smokey = DynamoCat(2, "Smokey", "female")
        val bandit = DynamoCat(2, "Bandit", "male")
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
        val toggles = DynamoCat(1, "Toggles", "female")
        val smokey = DynamoCat(2, "Smokey", "female")
        val bandit = DynamoCat(2, "Bandit", "male")
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(2))

        assertThat(mapper.query(expression)).containsExactly(bandit, smokey)
    }

    @Test
    fun `query in reverse order`() {
        val toggles = DynamoCat(1, "Toggles", "female")
        val smokey = DynamoCat(2, "Smokey", "female")
        val bandit = DynamoCat(2, "Bandit", "male")
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(2))
                .withScanIndexForward(false)

        assertThat(mapper.query(expression)).containsExactly(smokey, bandit)
    }

    @Test
    fun `get missing`() {
        val item = mapper.load(1, "Toggles")
        assertThat(item).isNull()
    }

    @Test
    fun get() {
        val toggles = DynamoCat(1, "Toggles", "female")
        mapper.save(toggles)

        assertThat(mapper.load(1, "Toggles")).isEqualTo(toggles)
    }

    @Test
    fun `delete item`() {
        val toggles = DynamoCat(1, "Toggles", "female")
        mapper.save(toggles)

        mapper.delete(toggles)
        assertThat(mapper.scan(DynamoDBScanExpression())).isEmpty()
    }

    @Test
    fun `delete missing item`() {
        val toggles = DynamoCat(1, "Toggles", "female")

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