package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.TestUtils.eq
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1ScanTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val cats = backend.createCatsTable()

    @Test
    fun `scan missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.scan("missingTable", emptyMap())
        }
    }

    @Test
    fun `scan with filter`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        client.scan(cats.name, mapOf("gender" to Condition().eq("male"))) shouldBe ScanResult()
            .withCount(1)
            .withItems(V1Fixtures.bandit)
    }
}