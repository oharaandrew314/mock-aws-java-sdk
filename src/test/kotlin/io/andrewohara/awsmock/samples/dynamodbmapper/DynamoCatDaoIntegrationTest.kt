package io.andrewohara.awsmock.samples.dynamodbmapper

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DynamoCatDaoIntegrationTest {

    private val testObj = DynamoCatsDao("cats", MockDynamoDbV1())

    init {
        testObj.mapper.createTable(ProvisionedThroughput(1, 1))
    }

    @Test
    fun `get missing cat`() {
        assertThat(testObj[1, "Toggles"]).isNull()
    }

    @Test
    fun `save and get cat`() {
        val toggles = DynamoCat(ownerId = 1, catName = "Toggles", gender = "female")
        testObj.save(toggles)

        assertThat(testObj[1, "Toggles"]).isEqualTo(toggles)
    }

    @Test
    fun `delete cat`() {
        val toggles = DynamoCat(ownerId = 1, catName = "Toggles", gender = "female")
        testObj.save(toggles)

        testObj.delete(toggles)

        assertThat(testObj[1, "Toggles"]).isNull()
    }

    @Test
    fun `delete missing cat`() {
        val toggles = DynamoCat(ownerId = 1, catName = "Toggles", gender = "female")
        testObj.delete(toggles)
    }

    @Test
    fun `list by owner with no cats`() {
        assertThat(testObj.list(1)).isEmpty()
    }

    @Test
    fun `list by owner`() {
        val smokey = DynamoCat(ownerId = 2, catName = "Smokey", gender = "female")
        testObj.save(smokey)

        val bandit = DynamoCat(ownerId = 2, catName = "Bandit", gender = "male")
        testObj.save(bandit)

        assertThat(testObj.list(2)).containsExactlyInAnyOrder(smokey, bandit)
    }
}