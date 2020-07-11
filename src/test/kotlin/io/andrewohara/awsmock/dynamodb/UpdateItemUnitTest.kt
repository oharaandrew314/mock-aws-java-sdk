package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsNotFound
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue
import io.andrewohara.awsmock.dynamodb.fixtures.CatsFixtures
import io.andrewohara.awsmock.dynamodb.fixtures.OwnersFixtures
import org.assertj.core.api.Assertions.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class UpdateItemUnitTest {

    private val client = MockAmazonDynamoDB()

    @Before
    fun setup() {
        CatsFixtures.createTable(client)
        OwnersFixtures.createTable(client)
    }

    @Test
    fun `update item for table that doesn't exist`() {
        val request = UpdateItemRequest()
                .withTableName("missingTable")
                .withKey(CatsFixtures.togglesKey)
                .withAttributeUpdates(mapOf(
                        "snoring" to AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(AttributeValue("loudly"))
                ))

        val exception = catchThrowableOfType({ client.updateItem(request) }, ResourceNotFoundException::class.java)

        exception.assertIsNotFound()
    }

    @Test
    fun `delete attribute from missing item via update`() {
        val request = UpdateItemRequest()
                .withTableName("cats")
                .withKey(CatsFixtures.togglesKey)
                .withAttributeUpdates(mapOf(
                        "snoring" to AttributeValueUpdate().withAction(AttributeAction.DELETE)
                ))

        client.updateItem(request)

        assertThat(client.getItem("cats", CatsFixtures.togglesKey).item).isNull()
    }

    @Test
    fun `delete attribute via update`() {
        client.putItem("cats", CatsFixtures.toggles)

        val request = UpdateItemRequest()
                .withTableName("cats")
                .withKey(CatsFixtures.togglesKey)
                .withAttributeUpdates(mapOf(
                        "gender" to AttributeValueUpdate().withAction(AttributeAction.DELETE)
                ))

        client.updateItem(request)

        assertThat(client.getItem("cats", CatsFixtures.togglesKey).item).isEqualTo(CatsFixtures.togglesKey)
    }

    @Test
    fun `update missing item`() {
        val request = UpdateItemRequest()
                .withTableName("cats")
                .withKey(CatsFixtures.togglesKey)
                .withAttributeUpdates(mapOf(
                        "snoring" to AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(AttributeValue("loudly"))
                ))

        client.updateItem(request)

        assertThat(client.getItem("cats", CatsFixtures.togglesKey).item).isEqualTo(
                CatsFixtures.togglesKey + mapOf("snoring" to AttributeValue("loudly"))
        )
    }

    @Test
    fun `update item`() {
        client.putItem("cats", CatsFixtures.toggles)

        val request = UpdateItemRequest()
                .withTableName("cats")
                .withKey(CatsFixtures.togglesKey)
                .withAttributeUpdates(mapOf(
                        "snoring" to AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(AttributeValue("loudly"))
                ))

        client.updateItem(request)

        assertThat(client.getItem("cats", CatsFixtures.togglesKey).item).isEqualTo(
                CatsFixtures.toggles + mapOf("snoring" to AttributeValue("loudly"))
        )
    }

    @Test
    fun `increment attribute`() {
        client.putItem("owners", OwnersFixtures.me)

        val request = UpdateItemRequest()
                .withTableName("owners")
                .withKey(OwnersFixtures.meKey)
                .withAttributeUpdates(mapOf(
                        "pets" to AttributeValueUpdate().withAction(AttributeAction.ADD).withValue(attributeValue(1))
                ))

        client.updateItem(request)

        val expected = OwnersFixtures.me.toMutableMap()
        expected["pets"] = attributeValue(2)

        assertThat(client.getItem("owners", OwnersFixtures.meKey).item).isEqualTo(expected)
    }
}