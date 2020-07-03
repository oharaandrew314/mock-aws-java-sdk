package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.junit.Test

class DynamoDevelopmentHelper {

    private val client = AmazonDynamoDBClientBuilder.defaultClient()

    @Test
    fun listTables() {
        val result = client.listTables()
        println(result)
    }

    @Test
    fun getItem() {
        val resp = client.getItem("ribbit-posts-prod-Posts-G4I7BVG8SL58", mapOf("subribbit" to AttributeValue("cats"), "id" to AttributeValue("C2NphKuhFtKdzDGg")))
        println(resp)
    }

}