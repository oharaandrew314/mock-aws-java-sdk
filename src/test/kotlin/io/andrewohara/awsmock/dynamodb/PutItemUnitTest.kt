package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMismatchedKey
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsMissingKey
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.Test

class PutItemUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `put item with missing hash key`() {
        CatsFixtures.createTable(client)

        val item = mapOf("foo" to AttributeValue("bar"))
        val exception = catchThrowableOfType({ client.putItem(CatsFixtures.tableName, item) }, AmazonDynamoDBException::class.java)

        exception.assertIsMissingKey(CatsFixtures.ownerIdAttribute)
    }

    @Test
    fun `put item with missing range key`() {
        CatsFixtures.createTable(client)

        val item = mapOf("ownerId" to AttributeValue().withN("1"))
        val exception = catchThrowableOfType({ client.putItem(CatsFixtures.tableName, item) }, AmazonDynamoDBException::class.java)

        exception.assertIsMissingKey(CatsFixtures.nameAttribute)
    }

    @Test
    fun `put item with hash key that doesn't match data type of schema`() {
        CatsFixtures.createTable(client)

        val item = mapOf("ownerId" to AttributeValue("1"), "name" to AttributeValue("Toggles"))
        val exception = catchThrowableOfType({ client.putItem(CatsFixtures.tableName, item) }, AmazonDynamoDBException::class.java)

        exception.assertIsMismatchedKey(CatsFixtures.ownerIdAttribute, ScalarAttributeType.S)
    }

    @Test
    fun `put item with range key that doesn't match data type of schema`() {
        CatsFixtures.createTable(client)

        val item = mapOf("ownerId" to AttributeValue().withN("1"), "name" to AttributeValue().withN("2"))
        val exception = catchThrowableOfType({ client.putItem(CatsFixtures.tableName, item) }, AmazonDynamoDBException::class.java)

        exception.assertIsMismatchedKey(CatsFixtures.nameAttribute, ScalarAttributeType.N)
    }

    @Test
    fun `put item`() {
        CatsFixtures.createTable(client)

        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        assertThat(client.getTable(CatsFixtures.tableName).items).containsExactly(CatsFixtures.toggles)
    }
}