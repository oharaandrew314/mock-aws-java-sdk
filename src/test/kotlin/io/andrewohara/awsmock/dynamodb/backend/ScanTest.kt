package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

class ScanTest {

    private val backend = MockDynamoBackend()
    private val cats = backend.createCatsTable()
    private val owners = backend.createOwnersTable()

    @Test
    fun `scan empty`() {
        cats.scan().shouldBeEmpty()
    }

    @Test
    fun `scan with no filter`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey)

        cats.scan().shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles,
            DynamoFixtures.smokey
        )
    }

    @Test
    fun `scan with filter`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "gender" to MockDynamoCondition.eq(MockDynamoValue(s = "male"))
        ).shouldContainExactly(
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N GT N`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            "pets" to MockDynamoCondition.gt(MockDynamoValue(1))
        ).shouldContainExactly(
            DynamoFixtures.parents
        )
    }

    @Test
    fun `scan for S CONTAINS S`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            "name" to MockDynamoCondition.contains(MockDynamoValue(s = "ren"))
        ).shouldContainExactly(
            DynamoFixtures.parents
        )
    }

    @Test
    fun `scan for S NOT_CONTAINS S`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            "name" to !MockDynamoCondition.contains(MockDynamoValue(s = "ren"))
        ).shouldContainExactly(
            DynamoFixtures.me
        )
    }

    @Test
    fun `scan for SS contains S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "features" to MockDynamoCondition.contains(MockDynamoValue(s = "grey"))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for NS contains N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "visitDates" to MockDynamoCondition.contains(MockDynamoValue(9001))
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N IN NS`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "ownerId" to MockDynamoCondition.inside(DynamoFixtures.parentsOwnerId, DynamoFixtures.meOwnerId)
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S IN SS`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "name" to MockDynamoCondition.inside(MockDynamoValue(s = "Smokey"), MockDynamoValue(s = "Bandit"))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N GE N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "ownerId" to MockDynamoCondition.ge(MockDynamoValue(2))
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N LE N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "ownerId" to MockDynamoCondition.le(MockDynamoValue(2))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N LT N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "ownerId" to MockDynamoCondition.lt(MockDynamoValue(2))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S NE S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "name" to !MockDynamoCondition.eq(MockDynamoValue(s = "Toggles"))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S BEGINS_WITH S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "name" to MockDynamoCondition.beginsWith(MockDynamoValue(s = "Tog"))
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N BETWEEN N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "ownerId" to MockDynamoCondition.between(MockDynamoValue(0), MockDynamoValue(10))
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("bestCat" to MockDynamoValue(s ="yes"))
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "bestCat" to MockDynamoCondition.exists()
        ).shouldContainExactly(
            toggles
        )
    }

    @Test
    fun `scan for S NOT EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("bestCat" to MockDynamoValue(s ="yes"))
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "bestCat" to !MockDynamoCondition.exists()
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for NULL EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("null" to MockDynamoValue())
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "null" to MockDynamoCondition.exists()
        ).shouldContainExactly(
            toggles
        )
    }

    @Test
    fun `scan for NULL NOT_EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("null" to MockDynamoValue())
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            "null" to !MockDynamoCondition.exists()
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }
}