package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.DeleteRequest
import com.amazonaws.services.dynamodbv2.model.PutRequest
import com.amazonaws.services.dynamodbv2.model.WriteRequest
import org.assertj.core.api.Assertions.*
import org.junit.Test

class BatchWriteUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `batch write to missing table`() {
        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(PutRequest(CatsFixtures.toggles))
        ))

        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEqualTo(request)
    }

    @Test
    fun `batch write new items`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)

        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(PutRequest(CatsFixtures.toggles)),
                WriteRequest(PutRequest(CatsFixtures.bandit))
        ))
        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEmpty()
        assertThat(client.getTable(CatsFixtures.tableName).items).containsExactlyInAnyOrder(CatsFixtures.bandit, CatsFixtures.toggles)
    }

    @Test
    fun `batch write for existing item`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(PutRequest(CatsFixtures.toggles))
        ))
        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEqualTo(request)  // TODO find out if this is true
    }

    @Test
    fun `batch write items to delete`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(DeleteRequest(CatsFixtures.toggles))
        ))
        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEmpty()
        assertThat(client.getTable(CatsFixtures.tableName).items).isEmpty()
    }
}