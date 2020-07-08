package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.eq
import org.assertj.core.api.Assertions.*
import org.junit.Test

class ScanUnitTest {

    private val client = MockAmazonDynamoDB()

    @Test
    fun `scan missing table`() {
        val exception = catchThrowableOfType(
                { client.scan(CatsFixtures.tableName, emptyMap()) },
                ResourceNotFoundException::class.java
        )

        exception.assertIsNotFound()
    }

    @Test
    fun `scan empty`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)

        val result = client.scan(CatsFixtures.tableName, emptyMap())

        assertThat(result.count).isEqualTo(0)
        assertThat(result.items).isEmpty()
    }

    @Test
    fun `scan with no filter`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)

        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)

        val result = client.scan(CatsFixtures.tableName, emptyMap())

        assertThat(result.count).isEqualTo(2)
        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.toggles, CatsFixtures.smokey)
    }

    @Test
    fun `scan with filter`() {
        CatsFixtures.createCatsTableByOwnerIdAndName(client)

        client.putItem(CatsFixtures.tableName, CatsFixtures.toggles)
        client.putItem(CatsFixtures.tableName, CatsFixtures.smokey)
        client.putItem(CatsFixtures.tableName, CatsFixtures.bandit)

        val result = client.scan(CatsFixtures.tableName, mapOf("gender" to Condition().eq("male")))

        assertThat(result.count).isEqualTo(1)
        assertThat(result.items).containsExactlyInAnyOrder(CatsFixtures.bandit)
    }
}