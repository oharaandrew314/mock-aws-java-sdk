package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.ListTablesResult
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1ListTablesUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)

    @Test
    fun `list tables when there are none`() {
        client.listTables() shouldBe ListTablesResult().withTableNames(emptyList())
    }

    @Test
    fun `list tables`() {
        val table = backend.createCatsTable()

        client.listTables() shouldBe ListTablesResult().withTableNames(table.name)
    }
}