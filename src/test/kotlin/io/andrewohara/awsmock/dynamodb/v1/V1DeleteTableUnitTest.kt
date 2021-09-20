package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import org.junit.jupiter.api.Test

class V1DeleteTableUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)

    @Test
    fun `delete table`() {
        val table = backend.createCatsTable()

        client.deleteTable(table.schema.name)

        backend.tables().shouldBeEmpty()
    }

    @Test
    fun `delete missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.deleteTable("missing")
        }
    }
}