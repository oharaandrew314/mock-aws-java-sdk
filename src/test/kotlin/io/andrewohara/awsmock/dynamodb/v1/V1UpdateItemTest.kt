package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class V1UpdateItemTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV1(backend)
    private val cats = backend.createCatsTable()

    @Test
    fun `update for table that doesn't exist`() {
        val request = UpdateItemRequest()
            .withTableName("missingTable")
            .withKey(V1Fixtures.togglesKey)
            .withAttributeUpdates(
                mapOf(
                    "snoring" to AttributeValueUpdate().withAction(AttributeAction.PUT)
                        .withValue(AttributeValue("loudly"))
                )
            )

        shouldThrow<ResourceNotFoundException> {
            client.updateItem(request)
        }
    }

    @Test
    fun `put value for item`() {
        cats.save(DynamoFixtures.toggles)

        val request = UpdateItemRequest()
            .withTableName(cats.name)
            .withKey(V1Fixtures.togglesKey)
            .withAttributeUpdates(mapOf(
                "snoring" to AttributeValueUpdate()
                    .withAction(AttributeAction.PUT)
                    .withValue(AttributeValue("loudly"))
            ))

        client.updateItem(request) shouldBe UpdateItemResult()
            .withAttributes(V1Fixtures.toggles.plus("snoring" to AttributeValue("loudly")))

        cats[DynamoFixtures.togglesKey] shouldBe DynamoFixtures.toggles.plus("snoring" to MockValue(s = "loudly"))
    }
}