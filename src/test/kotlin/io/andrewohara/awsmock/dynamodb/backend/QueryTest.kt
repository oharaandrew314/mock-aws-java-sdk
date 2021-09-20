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
            Conditions.eq(DynamoFixtures.togglesOwnerId).forAttribute("ownerId")
        ).shouldBeEmpty()
    }

    @Test
    fun `query table without sort key`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            Conditions.eq(DynamoFixtures.parentsOwnerId).forAttribute("ownerId")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `query table with sort key`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            setOf(Conditions.eq(DynamoFixtures.parentsOwnerId).forAttribute("ownerId")),
            scanIndexForward = false
        ).shouldContainExactly(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )

        table.query(
            setOf(Conditions.eq(DynamoFixtures.parentsOwnerId).forAttribute("ownerId")),
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
            setOf(Conditions.eq(MockValue(s = "Toggles")).forAttribute("name")),
            indexName = "names"
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

//    @Test
//    fun `query by global index with wrong key`() {
//        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)
//
//        shouldThrow<MockAwsException> {
//            table.query(
//                setOf(
//                    Conditions.eq(MockValue(s = "male")).forAttribute("gender")
//                ),
//                indexName = "names"
//            )
//        }
//    }

    @Test
    fun `query by local index`() {
        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        table.query(
            setOf(
                Conditions.eq(DynamoFixtures.parentsOwnerId).forAttribute("ownerId"),
                Conditions.eq(MockValue(s = "male")).forAttribute("gender")
            ),
            indexName = "genders"
        ). shouldContainExactly(
            DynamoFixtures.bandit
        )
    }

//    @Test
//    fun `query by local index without full key`() {
//        table.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)
//
//        shouldThrow<MockAwsException> {
//            table.query(
//                setOf(
//                    Conditions.eq(MockValue(s = "male")).forAttribute("gender")
//                ),
//                indexName = "genders"
//            )
//        }
//    }

    @Test
    fun `query by missing index`() {
        shouldThrow<MockAwsException> {
            table.query(
                setOf(Conditions.eq(MockValue(s = "male")).forAttribute("gender")),
                indexName = "missingIndex"
            )
        }
    }
}