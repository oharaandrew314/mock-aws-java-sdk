package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

class V2GetItemUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `get item from missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.getItem {
                it.tableName("missing")
                it.key(DynamoFixtures.togglesKey.toV2())
            }
        }
    }

    @Test
    fun `get missing item`() {
        client.getItem {
            it.tableName(table.name)
            it.key(DynamoFixtures.togglesKey.toV2())
        } shouldBe GetItemResponse.builder()
            .build()
    }

    @Test
    fun `get item`() {
        table.save(DynamoFixtures.toggles)

        client.getItem {
            it.tableName(table.name)
            it.key(DynamoFixtures.togglesKey.toV2())
        } shouldBe GetItemResponse.builder()
            .item(DynamoFixtures.toggles.toV2())
            .build()
    }

    @Test
    fun `get item with null attribute`() {
        val item = MockDynamoItem(
            "ownerId" to MockDynamoValue(n = 2),
            "name" to MockDynamoValue(s = "Toggles"),
            "age" to MockDynamoValue()
        )
        table.save(item)

        client.getItem {
            it.tableName(table.name)
            it.key(item.toV2())
        } shouldBe GetItemResponse.builder()
            .item(mapOf(
                "ownerId" to AttributeValue.builder().n("2").build(),
                "name" to AttributeValue.builder().s("Toggles").build(),
                "age" to AttributeValue.builder().nul(true).build()
            )).build()
    }
}