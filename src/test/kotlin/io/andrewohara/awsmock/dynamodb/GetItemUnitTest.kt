package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemRequest
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GetItemUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `get item from missing table`() {
        val exception = catchThrowableOfType(
                { client.getItem(CatsFixtures.tableName, mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles"))) },
                ResourceNotFoundException::class.java
        )
        exception.assertIsNotFound()
    }

    @Test
    fun `get missing item`() {
        CatsFixtures.createTable(client)

        val result = client.getItem(CatsFixtures.tableName, mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles")))
        assertThat(result.item).isNull()
    }

    @Test
    fun `get item`() {
        CatsFixtures.createTable(client)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val result = client.getItem(CatsFixtures.tableName, mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles")))
        assertThat(result.item).isEqualTo(CatsFixtures.toggles)
    }
}