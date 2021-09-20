package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
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
}