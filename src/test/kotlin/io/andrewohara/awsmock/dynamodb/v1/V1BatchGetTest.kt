package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class V1BatchGetTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)

    @Test
    fun `get from missing table`() {
        val keys = mapOf(
            "missing" to KeysAndAttributes().withKeys(V1Fixtures.togglesKey)
        )

        assertThrows<ResourceNotFoundException> {
            client.batchGetItem(keys)
        }
    }

    @Test
    fun `get items from multiple tables`() {
        val owners = backend.createOwnersTable()
        owners.save(DynamoFixtures.me)

        val cats = backend.createCatsTable()
        cats.save(DynamoFixtures.toggles)

        val keys = mapOf(
            owners.name to KeysAndAttributes().withKeys(
                listOf(V1Fixtures.meKey)
            ),
            cats.name to KeysAndAttributes().withKeys(
                listOf(V1Fixtures.togglesKey)
            )
        )

        client.batchGetItem(keys) shouldBe BatchGetItemResult()
            .withResponses(mapOf(
                owners.name to listOf(V1Fixtures.me),
                cats.name to listOf(V1Fixtures.toggles)
            ))
            .withUnprocessedKeys(emptyMap())
    }
}