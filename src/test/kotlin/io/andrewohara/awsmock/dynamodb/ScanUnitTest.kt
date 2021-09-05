package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue
import io.andrewohara.awsmock.dynamodb.TestUtils.eq
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.dynamodb.fixtures.OwnersFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class ScanUnitTest {

    private val client = MockAmazonDynamoDB()

    init {
        CatsFixtures.createTable(client)
        OwnersFixtures.createTable(client)
    }

    @Test
    fun `scan missing table`() {
        val exception = catchThrowableOfType(
                { client.scan("missingTable", emptyMap()) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsNotFound()
    }

    @Test
    fun `scan empty`() {
        val result = client.scan(CatsFixtures.tableName, emptyMap())

        assertThat(result.count).isEqualTo(0)
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `scan with no filter`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)

        val result = client.scan(CatsFixtures.tableName, emptyMap())

        assertThat(result.count).isEqualTo(2)
        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles, CatsFixtures.smokey)
    }

    @Test
    fun `scan with filter`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan(CatsFixtures.tableName, mapOf("gender" to Condition().eq("male")))

        assertThat(result.count).isEqualTo(1)
        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.bandit)
    }

    @Test
    fun `scan for N GT N`() {
        client.putItem("owners", OwnersFixtures.me)
        client.putItem("owners", OwnersFixtures.parents)

        val result = client.scan("owners", mapOf(
                "pets" to Condition(

                ).withComparisonOperator(ComparisonOperator.GT)
                        .withAttributeValueList(attributeValue(1))
        ))

        assertThat(result.items).containsExactly(OwnersFixtures.parents)
    }

    @Test
    fun `scan for S CONTAINS S`() {
        client.putItem("owners", OwnersFixtures.me)
        client.putItem("owners", OwnersFixtures.parents)

        val result = client.scan("owners", mapOf(
                "name" to Condition()
                        .withComparisonOperator(ComparisonOperator.CONTAINS)
                        .withAttributeValueList(AttributeValue("ren"))
        ))

        assertThat(result.items).containsExactly(OwnersFixtures.parents)
    }

    @Test
    fun `scan for S NOT_CONTAINS S`() {
        client.putItem("owners", OwnersFixtures.me)
        client.putItem("owners", OwnersFixtures.parents)

        val result = client.scan("owners", mapOf(
                "name" to Condition()
                        .withComparisonOperator(ComparisonOperator.NOT_CONTAINS)
                        .withAttributeValueList(AttributeValue("ren"))
        ))

        assertThat(result.items).containsExactly(OwnersFixtures.me)
    }

    @Test
    fun `scan for SS contains S`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "features" to Condition()
                        .withComparisonOperator(ComparisonOperator.CONTAINS)
                        .withAttributeValueList(AttributeValue("grey"))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.bandit, CatsFixtures.smokey)
    }

    @Test
    fun `scan for NS contains N`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "visitDates" to Condition()
                        .withComparisonOperator(ComparisonOperator.CONTAINS)
                        .withAttributeValueList(attributeValue(9001))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles)
    }

    @Test
    fun `scan for N IN NS`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "ownerId" to Condition()
                        .withComparisonOperator(ComparisonOperator.IN)
                        .withAttributeValueList(attributeValue(1), attributeValue(2))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles, CatsFixtures.smokey, CatsFixtures.bandit)
    }

    @Test
    fun `scan for S IN SS`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "name" to Condition()
                        .withComparisonOperator(ComparisonOperator.IN)
                        .withAttributeValueList(AttributeValue("Smokey"), AttributeValue("Bandit"))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.smokey, CatsFixtures.bandit)
    }

    @Test
    fun `scan for N GE N`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "ownerId" to Condition()
                        .withComparisonOperator(ComparisonOperator.GE)
                        .withAttributeValueList(attributeValue(2))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles)
    }

    @Test
    fun `scan for N LE N`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "ownerId" to Condition()
                        .withComparisonOperator(ComparisonOperator.LE)
                        .withAttributeValueList(attributeValue(2))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles, CatsFixtures.smokey, CatsFixtures.bandit)
    }

    @Test
    fun `scan for N LT N`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "ownerId" to Condition()
                        .withComparisonOperator(ComparisonOperator.LT)
                        .withAttributeValueList(attributeValue(2))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.smokey, CatsFixtures.bandit)
    }

    @Test
    fun `scan for S NE S`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "name" to Condition()
                        .withComparisonOperator(ComparisonOperator.NE)
                        .withAttributeValueList(AttributeValue("Toggles"))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.smokey, CatsFixtures.bandit)
    }

    @Test
    fun `scan for S BEGINS_WITH S`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "name" to Condition()
                        .withComparisonOperator(ComparisonOperator.BEGINS_WITH)
                        .withAttributeValueList(AttributeValue("Tog"))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles)
    }

    @Test
    fun `scan for N BETWEEN N`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "ownerId" to Condition()
                        .withComparisonOperator(ComparisonOperator.BETWEEN)
                        .withAttributeValueList(attributeValue(0), attributeValue(10))
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.smokey, CatsFixtures.bandit, CatsFixtures.toggles)
    }

    @Test
    fun `scan for S NOT_NULL`() {
        val toggles = CatsFixtures.toggles + mapOf("bestCat" to AttributeValue("yes"))
        client.putItem(CatsFixtures.tableName, toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "bestCat" to Condition()
                        .withComparisonOperator(ComparisonOperator.NOT_NULL)
        ))

        assertThat(result.items).containsExactlyInAnyOrder(toggles)
    }

    @Test
    fun `scan for S NULL`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles + mapOf("bestCat" to AttributeValue("yes")))
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan("cats", mapOf(
                "bestCat" to Condition()
                        .withComparisonOperator(ComparisonOperator.NULL)
        ))

        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.smokey, CatsFixtures.bandit)
    }
}