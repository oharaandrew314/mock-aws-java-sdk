package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.dynamodb.fixtures.OwnersFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchGetUnitTest {

    private val client = MockAmazonDynamoDB()

    init {
        CatsFixtures.createTable(client)
    }

    @Test
    fun `get from missing table`() {
        val keys = mapOf(
                "doggos" to KeysAndAttributes().withKeys(mapOf("name" to AttributeValue("Luna")))
        )

        val exception = catchThrowableOfType({ client.batchGetItem(keys) }, ResourceNotFoundException::class.java)
        exception.assertIsNotFound()
    }

    @Test
    fun `get missing items`() {
        val keys = mapOf(
                CatsFixtures.tableName to KeysAndAttributes().withKeys(mapOf(
                        "ownerId" to attributeValue(3),
                        "name" to AttributeValue("Mister")
                ))
        )

        val result = client.batchGetItem(keys)

        assertThat(result.responses).isEqualTo(mapOf(
                "cats" to emptyList<Map<String, AttributeValue>>()
        ))
        assertThat(result.unprocessedKeys).isEmpty()
    }

    @Test
    fun `get items`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)

        val keys = mapOf(
                CatsFixtures.tableName to KeysAndAttributes().withKeys(listOf(
                        mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles")),
                        mapOf("ownerId" to attributeValue(1), "name" to AttributeValue("Smokey"))
                ))
        )

        val result = client.batchGetItem(keys)

        assertThat(result.responses[CatsFixtures.tableName]).containsExactlyInAnyOrder(CatsFixtures.toggles, CatsFixtures.smokey)
    }

    @Test
    fun `get items from multiple tables`() {
        OwnersFixtures.createTable(client)

        client.putItem(OwnersFixtures.tableName, OwnersFixtures.me)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val keys = mapOf(
                CatsFixtures.tableName to KeysAndAttributes().withKeys(listOf(
                        mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles"))
                )),
                OwnersFixtures.tableName to KeysAndAttributes().withKeys(listOf(
                        mapOf("ownerId" to attributeValue(2))
                ))
        )

        val result = client.batchGetItem(keys)

        assertThat(result.responses).isEqualTo(mapOf(
                CatsFixtures.tableName to listOf(CatsFixtures.toggles),
                OwnersFixtures.tableName to listOf(OwnersFixtures.me)
        ))
    }
}