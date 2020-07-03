package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class TableMapperUnitTest {

//    private val mapper = DynamoCat.mapper(AmazonDynamoDBClientBuilder.defaultClient())
    private val mapper = DynamoCat.mapper(MockDynamoDB())

    @Before
    fun setup() {
        mapper.createTableIfNotExists(ProvisionedThroughput(1, 1))
    }

    @After
    fun tearDown() {
        for (item in mapper.scan(DynamoDBScanExpression())) {
            mapper.delete(item)
        }
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
    fun `scan with filter`() {
        val toggles = DynamoCat(1, "Toggles", "female")
        val smokey = DynamoCat(2, "Smokey", "female")
        val bandit = DynamoCat(2, "Bandit", "male")
        mapper.batchSave(setOf(toggles, smokey, bandit))

        val expression = DynamoDBScanExpression()
                .withScanFilter(mapOf("gender" to Condition().withComparisonOperator(ComparisonOperator.EQ).withAttributeValueList(AttributeValue("female"))))

        assertThat(mapper.scan(expression)).containsExactlyInAnyOrder(smokey, toggles)
    }

//    @Test
//    fun `query empty`() {
//
//    }

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

//    @Test
}