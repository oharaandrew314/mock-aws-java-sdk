package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import org.assertj.core.api.Assertions.*
import org.junit.Test

class DeleteTableUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `delete table`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)

        client.deleteTable(CatsFixtures.tableName)

        assertThat(client.getTableOrNull(CatsFixtures.tableName)).isNull()
    }

    @Test
    fun `delete missing table`() {
        val exception = catchThrowableOfType(
                { client.deleteTable(CatsFixtures.tableName) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsNotFound()
    }
}