package io.andrewohara.awsmock.dynamodb

import org.assertj.core.api.Assertions.*
import org.junit.Test

class ListTablesUnitTest {

    private val client = MockDynamoDB()

    @Test
    fun `list tables when there are none`() {
        assertThat(client.listTables().tableNames).isEmpty()
    }

    @Test
    fun `list tables`() {
        DynamoCat.createTable(client)

        assertThat(client.listTables().tableNames).containsExactly("cats")
    }
}