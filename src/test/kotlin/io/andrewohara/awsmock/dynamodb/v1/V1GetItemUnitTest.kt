package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1.Companion.toV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1GetItemUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val table = backend.createCatsTable()

    @Test
    fun `get item from missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.getItem("missing", V1Fixtures.togglesKey)
        }
    }

    @Test
    fun `get missing item`() {
        client.getItem(table.name, V1Fixtures.togglesKey) shouldBe GetItemResult()
    }

    @Test
    fun `get item`() {
        table.save(DynamoFixtures.toggles)

        client.getItem(table.name, V1Fixtures.togglesKey) shouldBe GetItemResult().withItem(V1Fixtures.toggles)
    }

    @Test
    fun `get item with null attribute`() {
        val item = MockDynamoItem(
            "ownerId" to MockDynamoValue(n = 2),
            "name" to MockDynamoValue(s = "Toggles"),
            "age" to MockDynamoValue()
        )
        table.save(item)

        client.getItem(table.name, item.toV1()) shouldBe GetItemResult()
            .withItem(mapOf(
                "ownerId" to AttributeValue().withN("2"),
                "name" to AttributeValue().withS("Toggles"),
                "age" to AttributeValue().withNULL(true)
            ))
    }
}