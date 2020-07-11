package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import com.amazonaws.services.dynamodbv2.model.TableStatus
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.samples.sqs.Cat
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
    fun `describe basic table`() {
        CatsFixtures.createTable(client)
        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)

        val result = client.describeTable(CatsFixtures.tableName)

        assertThat(result.table.tableName).isEqualTo(CatsFixtures.tableName)
        assertThat(result.table.tableArn).isNotEmpty()
        assertThat(result.table.tableId).isNotEmpty()
        assertThat(result.table.tableStatus).isEqualTo(TableStatus.ACTIVE.toString())
        assertThat(result.table.globalSecondaryIndexes).isEmpty()
        assertThat(result.table.localSecondaryIndexes).isEmpty()
        assertThat(result.table.provisionedThroughput).isEqualTo(ProvisionedThroughputDescription().withReadCapacityUnits(1).withWriteCapacityUnits(1))
        assertThat(result.table.itemCount).isEqualTo(1)
    }

    @Test
    fun `describe table with indexes`() {
        // TODO
    }
}