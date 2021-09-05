package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsInvalidParameter
import io.andrewohara.awsmock.dynamodb.TestUtils.assertTableInUse
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateTableUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `create table`() {
        val result = createDoggosTable()

        assertThat(result.tableDescription.tableName).isEqualTo("doggos")

        assertThat(client.listTables().tableNames).containsExactly("doggos")
    }

    @Test
    fun `create table that already exists`() {
        createDoggosTable()
        val exception = catchThrowableOfType(
                { createDoggosTable() },
                ResourceInUseException::class.java
        )
        exception.assertTableInUse("doggos")
    }

    @Test
    fun `create table where key schema doesn't have associated AttributeSchema`() {
        val exception = catchThrowableOfType(
                {
                    client.createTable(
                            listOf(
                                    AttributeDefinition("ownerId", ScalarAttributeType.N),
                                    AttributeDefinition("foo", ScalarAttributeType.S)
                            ),
                            "doggos",
                            listOf(
                                    KeySchemaElement("ownerId", KeyType.HASH),
                                    KeySchemaElement("doggoName", KeyType.RANGE)
                            ),
                            ProvisionedThroughput(1, 1)
                    )
                },
                AmazonDynamoDBException::class.java
        )

        exception.assertIsInvalidParameter()
    }

    @Test
    fun `create table with global indexes`() {
        val request = CreateTableRequest()
                .withTableName("doggos")
                .withAttributeDefinitions(
                        AttributeDefinition("ownerId", ScalarAttributeType.N),
                        AttributeDefinition("name", ScalarAttributeType.S)
                )
                .withKeySchema(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("name", KeyType.RANGE)
                )
                .withGlobalSecondaryIndexes(
                        GlobalSecondaryIndex()
                                .withIndexName("names")
                                .withKeySchema(KeySchemaElement("name", KeyType.HASH))
                                .withProjection(Projection().withProjectionType(ProjectionType.ALL))
                                .withProvisionedThroughput(ProvisionedThroughput(1, 1))
                )
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))

        val result = client.createTable(request)

        assertThat(result.tableDescription.globalSecondaryIndexes).hasSize(1)
    }

    @Test
    fun `create table with local indexes`() {
        val request = CreateTableRequest()
                .withTableName("doggos")
                .withAttributeDefinitions(
                        AttributeDefinition("ownerId", ScalarAttributeType.N),
                        AttributeDefinition("name", ScalarAttributeType.S),
                        AttributeDefinition("dob", ScalarAttributeType.S)
                )
                .withKeySchema(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("name", KeyType.RANGE)
                )
                .withLocalSecondaryIndexes(
                        LocalSecondaryIndex()
                                .withIndexName("names")
                                .withKeySchema(
                                        KeySchemaElement("ownerId", KeyType.HASH),
                                        KeySchemaElement("dob", KeyType.RANGE)
                                )
                                .withProjection(Projection().withProjectionType(ProjectionType.ALL))
                )
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))

        val result = client.createTable(request)

        assertThat(result.tableDescription.localSecondaryIndexes).hasSize(1)
    }

    private fun createDoggosTable(): CreateTableResult {
        return client.createTable(
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