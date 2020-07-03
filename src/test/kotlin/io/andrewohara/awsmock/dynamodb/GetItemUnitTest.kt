package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.DynamoAssertions.assertIsNotFound
import org.assertj.core.api.Assertions.*
import org.junit.Test

class GetItemUnitTest {

    private val client = MockDynamoDB()

    @Test
    fun `get item from missing table`() {
        val exception = catchThrowableOfType(
                { client.getItem(DynamoCat.tableName, mapOf("ownerId" to AttributeValue("1"), "catName" to AttributeValue("Toggles"))) },
                ResourceNotFoundException::class.java
        )
        exception.assertIsNotFound()
    }

    @Test
    fun `get missing item`() {
        DynamoCat.createTable(client)

        val result = client.getItem(DynamoCat.tableName, mapOf("ownerId" to AttributeValue("1"), "catName" to AttributeValue("Toggles")))
        assertThat(result.item).isNull()
    }

    @Test
    fun `get item`() {
        DynamoCat.createTable(client)
        DynamoCat.mapper(client).save(DynamoCat(1, "Toggles"))

        val result = client.getItem(DynamoCat.tableName, mapOf("ownerId" to AttributeValue().apply { n = "1" }, "catName" to AttributeValue("Toggles")))
        assertThat(result.item).isEqualTo(mapOf(
                "ownerId" to AttributeValue().apply { n = "1" },
                "catName" to AttributeValue("Toggles")
        ))
    }
}