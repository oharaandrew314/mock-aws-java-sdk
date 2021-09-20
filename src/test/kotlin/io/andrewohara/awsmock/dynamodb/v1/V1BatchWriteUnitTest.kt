package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1BatchWriteUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `batch write to missing table`() {
        val request = mapOf(
            "missingTable" to listOf(
                WriteRequest(PutRequest(V1Fixtures.toggles))
            )
        )

        shouldThrow<ResourceNotFoundException> {
            client.batchWriteItem(request)
        }
    }

    @Test
    fun `batch write new items`() {
        val request = mapOf(
            table.name to listOf(
                WriteRequest(PutRequest(V1Fixtures.toggles)),
                WriteRequest(PutRequest(V1Fixtures.bandit))
            )
        )

        client.batchWriteItem(request) shouldBe BatchWriteItemResult().withUnprocessedItems(emptyMap())

        table.items.shouldContainExactlyInAnyOrder(DynamoFixtures.bandit, DynamoFixtures.toggles)
    }

    @Test
    fun `batch write for existing item`() {
        table.save(DynamoFixtures.toggles)

        val request = mapOf(
            table.name to listOf(
                WriteRequest(PutRequest(V1Fixtures.toggles))
            )
        )
        client.batchWriteItem(request) shouldBe BatchWriteItemResult().withUnprocessedItems(emptyMap())

        table.items.shouldContainExactlyInAnyOrder(DynamoFixtures.toggles)
    }

    @Test
    fun `batch write items to delete`() {
        table.save(DynamoFixtures.togglesKey)

        val request = mapOf(
            table.name to listOf(
                WriteRequest(DeleteRequest(V1Fixtures.toggles))
            )
        )
        client.batchWriteItem(request) shouldBe BatchWriteItemResult().withUnprocessedItems(emptyMap())

        table.items.shouldBeEmpty()
    }

    @Test
    fun `batch delete missing items`() {
        val writeRequest = WriteRequest(DeleteRequest(V1Fixtures.toggles))
        val request = mapOf(table.name to listOf(writeRequest))

        client.batchWriteItem(request) shouldBe BatchWriteItemResult().withUnprocessedItems(emptyMap())
    }
}