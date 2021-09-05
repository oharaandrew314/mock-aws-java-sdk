package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.DeleteRequest
import com.amazonaws.services.dynamodbv2.model.PutRequest
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.amazonaws.services.dynamodbv2.model.WriteRequest
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class BatchWriteUnitTest {

    private val client = MockAmazonDynamoDB()

    init {
        CatsFixtures.createTable(client)
    }

    @Test
    fun `batch write to missing table`() {
        val request = mapOf("missingTable" to listOf(
                WriteRequest(PutRequest(CatsFixtures.toggles))
        ))

        val exception = catchThrowableOfType({ client.batchWriteItem(request) }, ResourceNotFoundException::class.java)
        exception.assertIsNotFound()
    }

    @Test
    fun `batch write new items`() {
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
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(PutRequest(CatsFixtures.toggles))
        ))
        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEmpty()
    }

    @Test
    fun `batch write items to delete`() {
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val request = mapOf(CatsFixtures.tableName to listOf(
                WriteRequest(DeleteRequest(CatsFixtures.toggles))
        ))
        val result = client.batchWriteItem(request)

        assertThat(result.unprocessedItems).isEmpty()
        assertThat(client.getTable(CatsFixtures.tableName).items).isEmpty()
    }
}