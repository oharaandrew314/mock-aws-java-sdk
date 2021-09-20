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
            Conditions.eq(MockDynamoValue(s = "male")).forAttribute("gender")
        ).shouldContainExactly(
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N GT N`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            Conditions.gt(MockDynamoValue(1)).forAttribute("pets")
        ).shouldContainExactly(
            DynamoFixtures.parents
        )
    }

    @Test
    fun `scan for S CONTAINS S`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            Conditions.contains(MockDynamoValue(s = "ren")).forAttribute("name")
        ).shouldContainExactly(
            DynamoFixtures.parents
        )
    }

    @Test
    fun `scan for S NOT_CONTAINS S`() {
        owners.save(DynamoFixtures.me, DynamoFixtures.parents)

        owners.scan(
            Conditions.contains(MockDynamoValue(s = "ren")).not().forAttribute("name")
        ).shouldContainExactly(
            DynamoFixtures.me
        )
    }

    @Test
    fun `scan for SS contains S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.contains(MockDynamoValue(s = "grey")).forAttribute("features")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey,
            DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for NS contains N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.contains(MockDynamoValue(9001)).forAttribute("visitDates")
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N IN NS`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions
                .inside(listOf(DynamoFixtures.parentsOwnerId, DynamoFixtures.meOwnerId))
                .forAttribute("ownerId")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S IN SS`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions
                .inside(listOf(MockDynamoValue(s = "Smokey"), MockDynamoValue(s = "Bandit")))
                .forAttribute("name")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N GE N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.ge(MockDynamoValue(2)).forAttribute("ownerId")
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N LE N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.le(MockDynamoValue(2)).forAttribute("ownerId")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for N LT N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.lt(MockDynamoValue(2)).forAttribute("ownerId")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S NE S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.eq(MockDynamoValue(s = "Toggles")).not().forAttribute("name")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S BEGINS_WITH S`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.beginsWith(MockDynamoValue(s = "Tog")).forAttribute("name")
        ).shouldContainExactly(
            DynamoFixtures.toggles
        )
    }

    @Test
    fun `scan for N BETWEEN N`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.between(MockDynamoValue(0)..MockDynamoValue(10)).forAttribute("ownerId")
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for S EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("bestCat" to MockDynamoValue(s ="yes"))
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.exists("bestCat")
        ).shouldContainExactly(
            toggles
        )
    }

    @Test
    fun `scan for S NOT EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("bestCat" to MockDynamoValue(s ="yes"))
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.exists("bestCat").inv()
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }

    @Test
    fun `scan for NULL EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("null" to MockDynamoValue())
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.exists("null"),
        ).shouldContainExactly(
            toggles
        )
    }

    @Test
    fun `scan for NULL NOT_EXISTS`() {
        val toggles = DynamoFixtures.toggles.plus("null" to MockDynamoValue())
        cats.save(toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        cats.scan(
            Conditions.exists("null").inv(),
        ).shouldContainExactlyInAnyOrder(
            DynamoFixtures.smokey, DynamoFixtures.bandit
        )
    }
}