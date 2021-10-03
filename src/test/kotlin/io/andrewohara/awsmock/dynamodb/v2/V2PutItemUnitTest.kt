package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse

class V2PutItemUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `put item with invalid hash key`() {
        val exception = shouldThrow<DynamoDbException> {
            client.putItem {
                it.tableName(table.name)
                it.item(mapOf("foo" to AttributeValue.builder().s("bar").build()))
            }
        }

        exception.message.shouldContain("One or more parameter values were invalid: Missing the key ownerId in the item")
    }

    @Test
    fun `put item with missing range key`() {
        val exception = shouldThrow<DynamoDbException> {
            client.putItem {
                it.tableName(table.name)
                it.item(mapOf("ownerId" to AttributeValue.builder().n("1").build()))
            }
        }

        exception.message.shouldContain("One or more parameter values were invalid: Missing the key name in the item")
    }

    @Test
    fun `put item with hash key that doesn't match data type of schema`() {
        val exception = shouldThrow<DynamoDbException> {
            client.putItem {
                it.tableName(table.name)
                it.item(mapOf(
                    "ownerId" to AttributeValue.builder().s("1").build(),
                    "name" to AttributeValue.builder().s("Toggles").build())
                )
            }
        }

        exception.message.shouldContain("One or more parameter values were invalid")
    }

    @Test
    fun `put item with range key that doesn't match data type of schema`() {
       val exception = shouldThrow<DynamoDbException> {
            client.putItem {
                it.tableName(table.name)
                it.item(mapOf(
                    "ownerId" to AttributeValue.builder().n("1").build(),
                    "name" to AttributeValue.builder().n("2").build()
                ))
            }

        }

        exception.awsErrorDetails().errorMessage() shouldBe "One or more parameter values were invalid: Type mismatch for key name expected: String actual: Number"
    }

    @Test
    fun `put item`() {
        client.putItem {
            it.tableName(table.name)
            it.item(DynamoFixtures.toggles.toV2())
        } shouldBe PutItemResponse.builder()
            .attributes(DynamoFixtures.toggles.toV2())
            .build()

        table.items.shouldContainExactly(DynamoFixtures.toggles)
    }

    @Test
    fun `put item with empty list`() {
        val item = DynamoFixtures.toggles.plus("features" to MockDynamoValue(list = emptyList()))

        client.putItem {
            it.tableName(table.name)
            it.item(item.toV2())
        } shouldBe PutItemResponse.builder()
            .attributes(item.toV2())
            .build()

        table.items.shouldContainExactly(item)
    }
}