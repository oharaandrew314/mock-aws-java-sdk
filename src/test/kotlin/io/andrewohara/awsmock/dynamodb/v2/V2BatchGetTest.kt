package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoAttribute
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

class V2BatchGetTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)

    @Test
    fun `get from missing table`() {
        assertThrows<ResourceNotFoundException> {
            client.batchGetItem {
                it.requestItems(mapOf(
                    "missing" to KeysAndAttributes.builder()
                        .keys(DynamoFixtures.togglesKey.toV2())
                        .build()
                ))
            }
        }
    }

    @Test
    fun `get oversized batch - truncate results`() {
        val table = backend.createTable("ids", MockDynamoAttribute(MockDynamoAttribute.Type.Number, "id"))
        val keys = (1..100).associate { id ->
            table.save(MockDynamoItem("id" to MockDynamoValue(id)))
            "id" to AttributeValue.builder().n(id.toString()).build()
        }

        val result = client.batchGetItem {
            it.requestItems(mapOf(
                "ids" to KeysAndAttributes.builder().keys(keys).build()
            ))
        }
        result.responses().shouldHaveSize(25)
        result.unprocessedKeys().shouldHaveSize(75)
    }

    @Test
    fun `get items from multiple tables`() {
        val owners = backend.createOwnersTable()
        owners.save(DynamoFixtures.me)

        val cats = backend.createCatsTable()
        cats.save(DynamoFixtures.toggles)

        client.batchGetItem {
            it.requestItems(mapOf(
                owners.name to KeysAndAttributes.builder()
                    .keys(DynamoFixtures.meKey.toV2())
                    .build(),
                cats.name to KeysAndAttributes.builder()
                    .keys(DynamoFixtures.togglesKey.toV2())
                    .build()
            ))
        } shouldBe BatchGetItemResponse.builder()
            .responses(mapOf(
                owners.name to listOf(DynamoFixtures.me.toV2()),
                cats.name to listOf(DynamoFixtures.toggles.toV2())
            ))
            .unprocessedKeys(emptyMap())
            .build()
    }
}