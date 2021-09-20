package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UpdateItemTest {

    private val backend = MockDynamoBackend()
    private val cats = backend.createCatsTable().also {
        it.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)
    }
    private val owners = backend.createOwnersTable().also {
        it.save(DynamoFixtures.me, DynamoFixtures.parents)
    }

    companion object {
        private val garrusKey = MockDynamoItem("ownerId" to DynamoFixtures.meOwnerId, "name" to MockDynamoValue(s = "Garrus"))
    }

    @Test
    fun `delete attribute for missing item`() {
        cats.update(
            key = garrusKey,
            updates = mapOf(
                "features" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Delete, value = null)
            )
        ).shouldBeNull()

        cats[garrusKey].shouldBeNull()
    }

    @Test
    fun `delete attribute via update`() {
        val expected = DynamoFixtures.toggles.minus("features")

        cats.update(
            key = DynamoFixtures.togglesKey,
            updates = mapOf(
                "features" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Delete, value = null)
            )
        ) shouldBe expected

        cats[DynamoFixtures.togglesKey] shouldBe expected
    }

    @Test
    fun `delete missing value from item`() {
        cats.update(
            DynamoFixtures.togglesKey,
            mapOf(
                "missing" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Delete, value = null)
            )
        ) shouldBe DynamoFixtures.toggles

        cats[DynamoFixtures.togglesKey] shouldBe DynamoFixtures.toggles
    }

    @Test
    fun `put value to missing item`() {
        val expected = garrusKey.plus("awesomeness" to MockDynamoValue(9001))

        cats.update(
            garrusKey,
            mapOf(
                "awesomeness" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Put, value = MockDynamoValue(9001))
            )
        ) shouldBe expected

        cats[garrusKey] shouldBe expected
    }

    @Test
    fun `put value to existing item`() {
        val expected = DynamoFixtures.toggles.plus("awesomeness" to MockDynamoValue(1337))

        cats.update(
            DynamoFixtures.togglesKey,
            mapOf(
                "awesomeness" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Put, value = MockDynamoValue(1337))
            )
        ) shouldBe expected

        cats[DynamoFixtures.togglesKey] shouldBe expected
    }

    @Test
    fun `increment value on existing item`() {
        val expected = DynamoFixtures.me.plus("pets" to MockDynamoValue(2))

        owners.update(
            DynamoFixtures.meKey,
            mapOf(
                "pets" to MockDynamoUpdate(action = MockDynamoUpdate.Type.Add, value = MockDynamoValue(1))
            )
        ) shouldBe expected

        owners[DynamoFixtures.meKey] shouldBe expected
    }
}