package io.andrewohara.awsmock.samples.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockAmazonDynamoDB
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class DynamoGamesDaoIntegrationTest {

    private val client = MockAmazonDynamoDB()
    private val testObj = DynamoGamesDao("games", client)

    init {
        val request = CreateTableRequest()
                .withTableName("games")
                .withAttributeDefinitions(
                        AttributeDefinition("id", ScalarAttributeType.N)
                )
                .withKeySchema(
                        KeySchemaElement("id", KeyType.HASH)
                )
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))

        client.createTable(request)
    }

    @Test
    fun `get missing game`() {
        assertThat(testObj[1]).isNull()
    }

    @Test
    fun `set and get game`() {
        testObj[1] = "Kingdom Come: Deliverance"
        testObj[2] = "Satisfactory"

        assertThat(testObj[1]).isEqualTo("Kingdom Come: Deliverance")
    }

    @Test
    fun `update game`() {
        testObj[0] = "Kingdom Come"
        testObj[0] = "Kingdom Come: Deliverance"

        assertThat(testObj[0]).isEqualTo("Kingdom Come: Deliverance")
    }
}