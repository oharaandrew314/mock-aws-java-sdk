package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMismatchedKey
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMissingKey
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class V1PutItemUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `put item with missing hash key`() {
        val item = mapOf("foo" to AttributeValue("bar"))

        shouldThrow<AmazonDynamoDBException> {
            client.putItem(table.name, item)
        }.assertIsMissingKey(V1Fixtures.ownerIdAttribute)
    }

    @Test
    fun `put item with missing range key`() {
        val item = mapOf("ownerId" to AttributeValue().withN("1"))
        shouldThrow<AmazonDynamoDBException> {
            client.putItem(table.name, item)
        }.assertIsMissingKey(V1Fixtures.nameAttribute)
    }

    @Test
    fun `put item with hash key that doesn't match data type of schema`() {
        val item = mapOf("ownerId" to AttributeValue("1"), "name" to AttributeValue("Toggles"))
        shouldThrow<AmazonDynamoDBException> {
            client.putItem(table.name, item)
        }.assertIsMismatchedKey()
    }

    @Test
    fun `put item with range key that doesn't match data type of schema`() {
        val item = mapOf(
            "ownerId" to AttributeValue().withN("1"),
            "name" to AttributeValue().withN("2")
        )
        shouldThrow<AmazonDynamoDBException> {
            client.putItem(table.name, item)
        }.assertIsMismatchedKey()
    }

    @Test
    fun `put item`() {
        client.putItem(table.name, V1Fixtures.toggles)

        table.items.shouldContainExactly(DynamoFixtures.toggles)
    }
}