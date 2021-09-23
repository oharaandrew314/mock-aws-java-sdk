package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException

class V2DeleteTableUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)

    @Test
    fun `delete table`() {
        val table = backend.createCatsTable()

        val result = client.deleteTable {
            it.tableName(table.name)
        }
        result.tableDescription().tableName() shouldBe table.name

        backend.tables().shouldBeEmpty()
    }

    @Test
    fun `delete missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.deleteTable {
                it.tableName("missingTable")
            }
        }
    }
}