package io.andrewohara.awsmock.samples.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeValue

class DynamoGamesDao(private val tableName: String, client: AmazonDynamoDB? = null) {

    private val dynamoClient = client ?: AmazonDynamoDBClientBuilder.defaultClient()

    operator fun get(id: Int): String? {
        val result = dynamoClient.getItem(tableName, mapOf(
                "id" to AttributeValue().withN(id.toString())
        ))

        return result.item?.getValue("name")?.s
    }

    operator fun set(id: Int, game: String) {
        dynamoClient.putItem(tableName, mapOf(
                "id" to AttributeValue().withN(id.toString()),
                "name" to AttributeValue(game)
        ))
    }
}