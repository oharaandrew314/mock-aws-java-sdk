package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BatchGetTest {

    private val backend = MockDynamoBackend()
    private val cats = backend.createCatsTable()
    private val owners = backend.createOwnersTable()

    @Test
    fun `get missing items`() {
        val requests = listOf(TableAndItem(cats.name, DynamoFixtures.togglesKey))

        backend.getAll(requests).results.shouldBeEmpty()
    }

    @Test
    fun `get items`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey)

        val requests = listOf(
            TableAndItem(cats.name, DynamoFixtures.togglesKey),
            TableAndItem(cats.name, DynamoFixtures.smokey)
        )

        backend.getAll(requests) shouldBe BatchGetItemResult(
            results = listOf(
                TableAndItem(cats.name, DynamoFixtures.toggles),
                TableAndItem(cats.name, DynamoFixtures.smokey)
            ),
            unprocessed = emptyList()
        )
    }

    @Test
    fun `get items from multiple tables`() {
        owners.save(DynamoFixtures.me)
        cats.save(DynamoFixtures.toggles)

        val keys = listOf(
            TableAndItem(owners.name, DynamoFixtures.meKey),
            TableAndItem(cats.name, DynamoFixtures.togglesKey)
        )

        backend.getAll(keys) shouldBe BatchGetItemResult(
            results = listOf(
                TableAndItem(owners.name, DynamoFixtures.me),
                TableAndItem(cats.name, DynamoFixtures.toggles)
            ),
            unprocessed = emptyList()
        )
    }
}