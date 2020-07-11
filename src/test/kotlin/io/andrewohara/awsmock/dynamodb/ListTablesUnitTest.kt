package io.andrewohara.awsmock.dynamodb

import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ListTablesUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `list tables when there are none`() {
        assertThat(client.listTables().tableNames).isEmpty()
    }

    @Test
    fun `list tables`() {
        CatsFixtures.createTable(client)

        assertThat(client.listTables().tableNames).containsExactly("cats")
    }
}