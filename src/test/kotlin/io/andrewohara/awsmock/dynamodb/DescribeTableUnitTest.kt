package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.dynamodb.fixtures.OwnersFixtures
import org.assertj.core.api.Assertions.*
import org.junit.Test

class DescribeTableUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `describe missing table`() {
        val exception = catchThrowableOfType(
                { client.describeTable("missingTable") },
                ResourceNotFoundException::class.java
        )

        exception.assertIsNotFound()
    }

    @Test
    fun `describe owners table with 0 items`() {
        OwnersFixtures.createTable(client)

        val table = client.describeTable(OwnersFixtures.tableName).table

        assertThat(table.tableName).isEqualTo(OwnersFixtures.tableName)
        assertThat(table.tableArn).isEqualTo("${OwnersFixtures.tableName}-arn")
        assertThat(table.tableId).isEqualTo("${OwnersFixtures.tableName}-id")
        assertThat(table.tableStatus).isEqualTo(TableStatus.ACTIVE.toString())
        assertThat(table.provisionedThroughput).isEqualTo(ProvisionedThroughputDescription().withReadCapacityUnits(1).withWriteCapacityUnits(1).withNumberOfDecreasesToday(0))
        assertThat(table.attributeDefinitions.toSet()).containsExactlyInAnyOrder(
                AttributeDefinition("ownerId", ScalarAttributeType.N)
        )
        assertThat(table.keySchema.toSet()).containsExactlyInAnyOrder(
                KeySchemaElement("ownerId", KeyType.HASH)
        )
        assertThat(table.itemCount).isEqualTo(0)

        assertThat(table.globalSecondaryIndexes).isEmpty()
        assertThat(table.localSecondaryIndexes).isEmpty()
    }

    @Test
    fun `describe cats table with 1 item`() {
        CatsFixtures.createTable(client)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val table = client.describeTable(CatsFixtures.tableName).table

        assertThat(table.tableName).isEqualTo(CatsFixtures.tableName)
        assertThat(table.tableArn).isEqualTo("${CatsFixtures.tableName}-arn")
        assertThat(table.tableId).isEqualTo("${CatsFixtures.tableName}-id")
        assertThat(table.tableStatus).isEqualTo(TableStatus.ACTIVE.toString())
        assertThat(table.provisionedThroughput).isEqualTo(ProvisionedThroughputDescription().withReadCapacityUnits(1).withWriteCapacityUnits(1).withNumberOfDecreasesToday(0))
        assertThat(table.attributeDefinitions.toSet()).containsExactlyInAnyOrder(
                AttributeDefinition("gender", ScalarAttributeType.S),
                AttributeDefinition("name", ScalarAttributeType.S),
                AttributeDefinition("ownerId", ScalarAttributeType.N)
        )
        assertThat(table.keySchema.toSet()).containsExactlyInAnyOrder(
                KeySchemaElement("ownerId", KeyType.HASH),
                KeySchemaElement("name", KeyType.RANGE)
        )
        assertThat(table.itemCount).isEqualTo(1)

        assertThat(table.globalSecondaryIndexes).containsExactly(
                GlobalSecondaryIndexDescription().apply {
                    indexName = "names"
                    indexArn = "names-arn"
                    indexStatus = IndexStatus.ACTIVE.toString()
                    projection = Projection().withProjectionType(ProjectionType.KEYS_ONLY)
                    itemCount = 1
                    provisionedThroughput = ProvisionedThroughputDescription().withReadCapacityUnits(1).withWriteCapacityUnits(1).withNumberOfDecreasesToday(0)

                    withKeySchema(
                            KeySchemaElement("name", KeyType.HASH)
                    )
                }
        )
        assertThat(table.localSecondaryIndexes).containsExactly(
                LocalSecondaryIndexDescription().apply {
                    indexName = "genders"
                    indexArn = "genders-arn"
                    projection = Projection().withProjectionType(ProjectionType.KEYS_ONLY)
                    itemCount = 1

                    withKeySchema(
                            KeySchemaElement("ownerId", KeyType.HASH),
                            KeySchemaElement("gender", KeyType.RANGE)
                    )
                }
        )
    }
}