package io.andrewohara.awsmock.samples.dynamodbmapper

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DynamoCatDaoIntegrationTest {

    private val backend = MockDynamoBackend()

    private val mapper = DynamoCat.mapper(MockDynamoDbV1(backend)).also {
        it.createTable(ProvisionedThroughput(1, 1))
    }

    val testObj = DynamoCatsDao(mapper)

    object Fixtures {
        val toggles = DynamoCat(ownerId = 1, catName = "Toggles", gender = "female")
        val smokey = DynamoCat(ownerId = 2, catName = "Smokey", gender = "female")
        val bandit = DynamoCat(ownerId = 2, catName = "Bandit", gender = "male")
    }

    @Test
    fun `get missing cat`() {
        testObj[1, "Toggles"].shouldBeNull()
    }

    @Test
    fun `get cat`() {
        mapper.save(Fixtures.toggles)

        testObj[1, "Toggles"] shouldBe Fixtures.toggles
    }

    @Test
    fun `delete cat`() {
        mapper.save(Fixtures.toggles)

        testObj.delete(Fixtures.toggles)

        mapper.load(1, "Toggles").shouldBeNull()
    }

    @Test
    fun `delete missing cat`() {
        testObj.delete(Fixtures.toggles)
    }

    @Test
    fun `list by owner`() {
        mapper.save(Fixtures.smokey)
        mapper.save(Fixtures.bandit)

        testObj.list(2).shouldContainExactlyInAnyOrder(
            Fixtures.smokey, Fixtures.bandit
        )
    }
}