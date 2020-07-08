package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.*

object CatsFixtures {

    const val tableName = "cats"

    val ownerIdAttribute = AttributeDefinition("ownerId", ScalarAttributeType.N)
    val nameAttribute = AttributeDefinition("name", ScalarAttributeType.S)

    fun createCatsTableByName(client: AmazonDynamoDB) {
        client.createTable(
                listOf(nameAttribute),
                tableName,
                listOf(KeySchemaElement("name", KeyType.HASH)),
                ProvisionedThroughput(1, 1)
        )
    }

    fun createCatsTableByOwnerIdAndName(client: AmazonDynamoDB) {
        client.createTable(
                listOf(ownerIdAttribute, nameAttribute),
                tableName,
                listOf(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("name", KeyType.RANGE)
                ),
                ProvisionedThroughput(1, 1)
        )
    }

    val smokey = mapOf("ownerId" to AttributeValue().withN("1"), "name" to AttributeValue("Smokey"), "gender" to AttributeValue("female"))
    val bandit = mapOf("ownerId" to AttributeValue().withN("1"), "name" to AttributeValue("Bandit"), "gender" to AttributeValue("male"))
    val toggles = mapOf("ownerId" to AttributeValue().withN("2"), "name" to AttributeValue("Toggles"), "gender" to AttributeValue("female"))
}