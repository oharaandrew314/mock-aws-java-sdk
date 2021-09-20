package io.andrewohara.awsmock.samples.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeValue

class DynamoGamesDao(private val tableName: String, private val client: AmazonDynamoDB) {

    operator fun get(id: Int): String? {
        val result = client.getItem(
            tableName, mapOf(
                "id" to AttributeValue().withN(id.toString())
            )
        )

        return result.item?.getValue("name")?.s
    }

    operator fun set(id: Int, game: String) {
        client.putItem(
            tableName, mapOf(
                "id" to AttributeValue().withN(id.toString()),
                "name" to AttributeValue(game)
            )
        )
    }
}