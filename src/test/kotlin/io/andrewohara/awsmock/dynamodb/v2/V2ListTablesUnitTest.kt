package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse

class V2ListTablesUnitTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)

    @Test
    fun `list tables when there are none`() {
        client.listTables() shouldBe ListTablesResponse.builder()
            .tableNames(emptyList())
            .build()
    }

    @Test
    fun `list tables`() {
        val cats = backend.createCatsTable()
        val owners = backend.createOwnersTable()

        client.listTables() shouldBe ListTablesResponse.builder()
            .tableNames(cats.name, owners.name)
            .build()
    }

    @Test
    fun `list tables with limit`() {
        val cats = backend.createCatsTable()
        backend.createOwnersTable()

        client.listTables {
            it.limit(1)
        } shouldBe ListTablesResponse.builder()
            .tableNames(cats.name)
            .build()
    }
}