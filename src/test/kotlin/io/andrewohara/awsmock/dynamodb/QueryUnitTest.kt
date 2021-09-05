package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMissingIndex
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.eq
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.dynamodb.fixtures.OwnersFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class QueryUnitTest {

    private val client = MockAmazonDynamoDB()

    init {
        CatsFixtures.createTable(client)
    }

    @Test
    fun `query missing table`() {
        val request = QueryRequest()
                .withTableName("missingTable")
                .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))

        val exception = catchThrowableOfType(
                { client.query(request) },
                ResourceNotFoundException::class.java
        )
        exception.assertIsNotFound()
    }

    @Test
    fun `query empty table`() {
        val request = QueryRequest()
                .withTableName(CatsFixtures.tableName)
                .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))
        val result = client.query(request)

        assertThat(result.count).isEqualTo(0)
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `query table without sort key`() {
        OwnersFixtures.createTable(client)

        client.putItem(OwnersFixtures.tableName, OwnersFixtures.parents)
        client.putItem(OwnersFixtures.tableName, OwnersFixtures.me)

        val request = QueryRequest()
                .withTableName(OwnersFixtures.tableName)
                .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))
        val result = client.query(request)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.items).containsExactly(OwnersFixtures.parents)
    }

    @Test
    fun `query table with sort key`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = QueryRequest()
                .withTableName(CatsFixtures.tableName)
                .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))
        val result = client.query(request)

        assertThat(result.count).isEqualTo(2)
        assertThat(result.items).containsExactly(CatsFixtures.bandit, CatsFixtures.smokey)
    }

    @Test
    fun `query by global index`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = QueryRequest()
                .withTableName(CatsFixtures.tableName)
                .withIndexName("names")
                .withKeyConditions(mapOf("name" to Condition().eq("Toggles")))


        val result = client.query(request)

        assertThat(result.count).isEqualTo(1)
        assertThat(result.items).containsExactly(CatsFixtures.toggles)
    }

    @Test
    fun `query by local index`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = QueryRequest()
                .withTableName(CatsFixtures.tableName)
                .withIndexName("genders")
                .withKeyConditions(mapOf("gender" to Condition().eq("female")))

        val result = client.query(request)

        assertThat(result.count).isEqualTo(2)
        assertThat(result.items).containsExactly(CatsFixtures.smokey, CatsFixtures.toggles)
    }

    @Test
    fun `query by missing index`() {
        val request = QueryRequest()
                .withTableName(CatsFixtures.tableName)
                .withIndexName("foos")
                .withKeyConditions(mapOf("gender" to Condition().eq("female")))

        val exception = catchThrowableOfType(
                { client.query(request) },
                AmazonDynamoDBException::class.java
        )

        exception.assertIsMissingIndex("foos")
    }
}