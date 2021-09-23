package io.andrewohara.awsmock.samples.dynamodb

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoAttribute
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DynamoGamesDaoIntegrationTest {

    private val backend = MockDynamoBackend()
    private val testObj = DynamoGamesDao("games", MockDynamoDbV1(backend))
    val table = backend.createTable(
        name = "games",
        hashKey = MockDynamoAttribute(MockDynamoAttribute.Type.Number,"id"),
    )

    object Fixtures {
        val massEffect3 = MockDynamoItem(
            "id" to MockDynamoValue(123),
            "name" to MockDynamoValue(s = "Mass Effect 3")
        )
    }

    @Test
    fun `get missing game`() {
        testObj[1].shouldBeNull()
    }

    @Test
    fun `get game`() {
        table.save(Fixtures.massEffect3)

        testObj[123] shouldBe "Mass Effect 3"
    }

    @Test
    fun `add game`() {
        testObj[123] = "Mass Effect 3"

        table.items.shouldContainExactlyInAnyOrder(
            Fixtures.massEffect3
        )
    }

    @Test
    fun `update game`() {
        table.save(Fixtures.massEffect3)

        testObj[123] = "Autonauts"

        table.items.shouldContainExactlyInAnyOrder(
            MockDynamoItem(
                "id" to MockDynamoValue(123),
                "name" to MockDynamoValue(s = "Autonauts")
            )
        )
    }
}