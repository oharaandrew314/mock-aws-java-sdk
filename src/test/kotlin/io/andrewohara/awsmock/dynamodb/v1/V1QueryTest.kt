package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMissingIndex
import io.andrewohara.awsmock.dynamodb.TestUtils.eq
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1QueryTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val table = backend.createCatsTable().apply {
        save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)
    }

    @Test
    fun `query missing table`() {
        val request = QueryRequest()
            .withTableName("missingTable")
            .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))

        shouldThrow<ResourceNotFoundException> {
            client.query(request)
        }
    }

    @Test
    fun `query only`() {
        val request = QueryRequest()
            .withTableName(table.name)
            .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))
            .withScanIndexForward(false)

        client.query(request) shouldBe QueryResult()
            .withCount(2)
            .withItems(V1Fixtures.smokey, V1Fixtures.bandit)
    }

    @Test
    fun `query by missing index`() {
        val request = QueryRequest()
            .withTableName(table.name)
            .withIndexName("foos")
            .withKeyConditions(mapOf("gender" to Condition().eq("female")))

        shouldThrow<AmazonDynamoDBException> {
            client.query(request)
        }.assertIsMissingIndex("foos")
    }

    @Test
    fun `query and filter`() {
        val request = QueryRequest()
            .withTableName(table.name)
            .withKeyConditions(mapOf("ownerId" to Condition().eq(1)))
            .withFilterExpression("gender = :gender")
            .withExpressionAttributeValues(mapOf(":gender" to AttributeValue("female")))
            .withScanIndexForward(false)

        client.query(request) shouldBe QueryResult()
            .withCount(1)
            .withItems(V1Fixtures.smokey)
    }
}