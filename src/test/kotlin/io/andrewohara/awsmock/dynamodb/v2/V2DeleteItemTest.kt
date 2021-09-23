package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

class V2DeleteItemTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)

    @Test
    fun `delete item from missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.deleteItem {
                it.tableName("missingTable")
                it.key(DynamoFixtures.togglesKey.toV2())
            }
        }
    }

    @Test
    fun `delete item from table`() {
        val table = backend.createCatsTable()
        table.save(DynamoFixtures.toggles)

        client.deleteItem {
            it.tableName(table.name)
            it.key(DynamoFixtures.togglesKey.toV2())
        } shouldBe DeleteItemResponse.builder()
            .attributes(DynamoFixtures.toggles.toV2())
            .build()

        table[DynamoFixtures.togglesKey].shouldBeNull()
    }

    @Test
    fun `delete missing item from table`() {
        val table = backend.createCatsTable()

        client.deleteItem {
            it.tableName(table.name)
            it.key(DynamoFixtures.togglesKey.toV2())
        } shouldBe DeleteItemResponse.builder()
            .build()

        table[DynamoFixtures.togglesKey].shouldBeNull()
    }
}