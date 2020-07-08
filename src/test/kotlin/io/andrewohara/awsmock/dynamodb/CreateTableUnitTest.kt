package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertTableInUse
import org.assertj.core.api.Assertions
import org.junit.Test

class CreateTableUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `create table`() {
        createDoggosTable()

        Assertions.assertThat(client.listTables().tableNames).containsExactly("doggos")
    }

    @Test
    fun `create table that already exists`() {
        createDoggosTable()
        val exception = Assertions.catchThrowableOfType(
                { createDoggosTable() },
                ResourceInUseException::class.java
        )
        exception.assertTableInUse("doggos")
    }

    @Test
    fun `create table where key schema doesn't have associated AttributeSchema`() {
        // TODO
    }

    private fun createDoggosTable() {
        client.createTable(
                listOf(
                        AttributeDefinition("ownerId", ScalarAttributeType.N),
                        AttributeDefinition("doggoName", ScalarAttributeType.S)
                ),
                "doggos",
                listOf(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("doggoName", KeyType.RANGE)
                ),
                ProvisionedThroughput(1, 1)
        )
    }
}