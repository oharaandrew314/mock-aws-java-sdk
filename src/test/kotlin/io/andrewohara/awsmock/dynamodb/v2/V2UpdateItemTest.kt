package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.*

class V2UpdateItemTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val cats = backend.createCatsTable()

    @Test
    fun `update for table that doesn't exist`() {
        shouldThrow<ResourceNotFoundException> {
            client.updateItem {
                it.tableName("missingTable")
                it.key(DynamoFixtures.togglesKey.toV2())
                it.attributeUpdates(mapOf(
                    "snoring" to AttributeValueUpdate.builder()
                        .action(AttributeAction.PUT)
                        .value(AttributeValue.builder().s("loudly").build())
                        .build()
                ))
            }
        }
    }

    @Test
    fun `update with null updates`() {
        cats.save(DynamoFixtures.toggles)

        client.updateItem {
            it.tableName(cats.name)
            it.key(DynamoFixtures.togglesKey.toV2())
        } shouldBe UpdateItemResponse.builder()
            .attributes(DynamoFixtures.toggles.toV2())
            .build()

        cats[DynamoFixtures.togglesKey] shouldBe DynamoFixtures.toggles
    }

    @Test
    fun `put value for item`() {
        cats.save(DynamoFixtures.toggles)

        val expected = DynamoFixtures.toggles
            .plus("snoring" to MockDynamoValue("loudly"))

        client.updateItem {
            it.tableName(cats.name)
            it.key(DynamoFixtures.togglesKey.toV2())
            it.attributeUpdates(mapOf(
                "snoring" to AttributeValueUpdate.builder()
                    .action(AttributeAction.PUT)
                    .value(AttributeValue.builder().s("loudly").build())
                    .build()
            ))
        } shouldBe UpdateItemResponse.builder()
            .attributes(expected.toV2())
            .build()

        cats[DynamoFixtures.togglesKey] shouldBe expected
    }
}