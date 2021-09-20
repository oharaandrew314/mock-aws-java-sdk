package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.DeleteItemResult
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1DeleteItemTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)

    @Test
    fun `delete item from missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.deleteItem("missingTable", V1Fixtures.togglesKey)
        }
    }

    @Test
    fun `delete item from table`() {
        val table = backend.createCatsTable()
        table.save(DynamoFixtures.toggles)

        client.deleteItem(table.name, V1Fixtures.togglesKey) shouldBe(
            DeleteItemResult().withAttributes(V1Fixtures.toggles)
        )

        table[DynamoFixtures.togglesKey].shouldBeNull()
    }

    @Test
    fun `delete missing item from table`() {
        val table = backend.createCatsTable()

        client.deleteItem(table.name, V1Fixtures.togglesKey) shouldBe DeleteItemResult()

        table[DynamoFixtures.togglesKey].shouldBeNull()
    }
}