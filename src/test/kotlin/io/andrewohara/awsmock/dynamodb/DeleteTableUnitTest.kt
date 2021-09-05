package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DeleteTableUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `delete table`() {
        CatsFixtures.createTable(client)

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