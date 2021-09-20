package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BatchGetTest {

    private val backend = MockDynamoBackend()
    private val cats = backend.createCatsTable()
    private val owners = backend.createOwnersTable()

    @Test
    fun `get missing items`() {
        val keys = mapOf(cats.name to listOf(DynamoFixtures.togglesKey))

        backend.getAll(keys).shouldBeEmpty()
    }

    @Test
    fun `get items`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey)

        val keys = mapOf(cats.name to listOf(DynamoFixtures.togglesKey, DynamoFixtures.smokey))

        backend.getAll(keys) shouldBe mapOf(
            cats.name to listOf(DynamoFixtures.toggles, DynamoFixtures.smokey)
        )
    }

    @Test
    fun `get items from multiple tables`() {
        owners.save(DynamoFixtures.me)
        cats.save(DynamoFixtures.toggles)

        val keys = mapOf(
            owners.name to listOf(DynamoFixtures.meKey),
            cats.name to listOf(DynamoFixtures.togglesKey)
        )

        backend.getAll(keys) shouldBe mapOf(
            owners.name to listOf(DynamoFixtures.me),
            cats.name to listOf(DynamoFixtures.toggles)
        )
    }
}