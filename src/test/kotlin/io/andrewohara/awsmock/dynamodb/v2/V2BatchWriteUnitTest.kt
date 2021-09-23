package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.*

class V2BatchWriteUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `batch write to missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.batchWriteItem {
                it.requestItems(mapOf(
                    "missingTable" to listOf(
                        WriteRequest.builder()
                            .put(DynamoFixtures.toggles)
                            .build()
                    )
                ))
            }
        }
    }

    @Test
    fun `batch write new items`() {
        client.batchWriteItem {
            it.requestItems(mapOf(
                table.name to listOf(
                    WriteRequest.builder()
                        .put(DynamoFixtures.toggles)
                        .build(),
                    WriteRequest.builder()
                        .put(DynamoFixtures.bandit)
                        .build()
                )
            ))
        } shouldBe BatchWriteItemResponse.builder()
            .unprocessedItems(emptyMap())
            .build()

        table.items.shouldContainExactlyInAnyOrder(DynamoFixtures.bandit, DynamoFixtures.toggles)
    }

    @Test
    fun `batch write for existing item`() {
        table.save(DynamoFixtures.toggles)

        client.batchWriteItem {
            it.requestItems(mapOf(
                table.name to listOf(
                    WriteRequest.builder()
                        .put(DynamoFixtures.toggles)
                        .build()
                )
            ))
        } shouldBe BatchWriteItemResponse.builder()
            .unprocessedItems(emptyMap())
            .build()

        table.items.shouldContainExactlyInAnyOrder(DynamoFixtures.toggles)
    }

    @Test
    fun `batch write items to delete`() {
        table.save(DynamoFixtures.togglesKey)

        client.batchWriteItem {
            it.requestItems(mapOf(
                table.name to listOf(
                    WriteRequest.builder()
                        .del(DynamoFixtures.togglesKey)
                        .build()
                )
            ))
        } shouldBe BatchWriteItemResponse.builder()
            .unprocessedItems(emptyMap())
            .build()

        table.items.shouldBeEmpty()
    }

    @Test
    fun `batch delete missing items`() {
        client.batchWriteItem {
            it.requestItems(mapOf(
                table.name to listOf(
                    WriteRequest.builder()
                        .del(DynamoFixtures.togglesKey)
                        .build()
                )
            ))
        } shouldBe BatchWriteItemResponse.builder()
            .unprocessedItems(emptyMap())
            .build()
    }

    companion object {
        private fun WriteRequest.Builder.put(item: MockDynamoItem) = putRequest(PutRequest.builder().item(item.toV2()).build())
        private fun WriteRequest.Builder.del(item: MockDynamoItem) = deleteRequest(DeleteRequest.builder().key(item.toV2()).build())
    }
}