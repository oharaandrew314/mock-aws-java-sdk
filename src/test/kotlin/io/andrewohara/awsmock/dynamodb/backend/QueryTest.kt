package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

class QueryTest {

    private val backend = MockDynamoBackend()
    private val table = backend.createCatsTable()

    @Test
    fun `query empty table`() {
        table.query(
            "ownerId" to MockDynamoCondition.eq(DynamoFixtures.togglesOwnerId)
        ).shouldBeEmpty()
    }

    @Test
    fun `query table without sort key`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            "ownerId" to MockDynamoCondition.eq(DynamoFixtures.parentsOwnerId)
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `query table with sort key`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            mapOf(
                "ownerId" to MockDynamoCondition.eq(DynamoFixtures.parentsOwnerId)
            ),
            scanIndexForward = false
        ).shouldContainExactly(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )

        table.query(
            mapOf(
                "ownerId" to MockDynamoCondition.eq(DynamoFixtures.parentsOwnerId)
            ),
            scanIndexForward = true
        ).shouldContainExactly(
            DynamoFixtures.bandit,
            DynamoFixtures.smokey
        )
    }

    @Test
    fun `query by global index`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            mapOf(
                "name" to MockDynamoCondition.eq(MockDynamoValue(s = "Toggles"))
            ),
            indexName = "names"
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `query many by global index - forward order`() {
        val table = PeopleTable.create(backend)
        table.save(PeopleTable.johnDoe, PeopleTable.janeDoe, PeopleTable.billSmith)

        table.query(
            mapOf(
                "lastName" to MockDynamoCondition.eq(MockDynamoValue(s = "Doe"))
            ),
            scanIndexForward = true,
            indexName = "names"
        ).shouldContainExactly(PeopleTable.janeDoe, PeopleTable.johnDoe)
    }

    @Test
    fun `query many by global index - backward order`() {
        val table = PeopleTable.create(backend)
        table.save(PeopleTable.johnDoe, PeopleTable.janeDoe, PeopleTable.billSmith)

        table.query(
            mapOf(
                "lastName" to MockDynamoCondition.eq(MockDynamoValue(s = "Doe"))
            ),
            scanIndexForward = false,
            indexName = "names"
        ).shouldContainExactly(PeopleTable.johnDoe, PeopleTable.janeDoe)
    }

    @Test
    fun `query by global index with wrong key`() {
        shouldThrow<MockAwsException> {
            table.query(
                mapOf(
                    "gender" to MockDynamoCondition.eq(MockDynamoValue(s = "male"))
                ),
                indexName = "names"
            )
        }
    }

    @Test
    fun `query by local index`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            mapOf(
                "ownerId" to MockDynamoCondition.eq(DynamoFixtures.parentsOwnerId),
                "gender" to MockDynamoCondition.eq(MockDynamoValue(s = "male"))
            ),
            indexName = "genders"
        ). shouldContainExactly(
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `query by missing index`() {
        shouldThrow<MockAwsException> {
            table.query(
                mapOf(
                    "gender" to MockDynamoCondition.eq(MockDynamoValue(s = "male"))
                ),
                indexName = "missingIndex"
            )
        }
    }
}