package io.andrewohara.awsmock.dynamodb.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue

object CatsFixtures {

    const val tableName = "cats"

    val ownerIdAttribute = AttributeDefinition("ownerId", ScalarAttributeType.N)
    val nameAttribute = AttributeDefinition("name", ScalarAttributeType.S)

    val togglesKey = mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Toggles"))

    val smokey = mapOf("ownerId" to attributeValue(1), "name" to AttributeValue("Smokey"), "gender" to AttributeValue("female"))
    val bandit = mapOf("ownerId" to attributeValue(1), "name" to AttributeValue("Bandit"), "gender" to AttributeValue("male"))
    val toggles = togglesKey + mapOf("gender" to AttributeValue("female"))

    fun createTable(client: AmazonDynamoDB) {
        mapper(client).createTable(ProvisionedThroughput(1, 1))
    }

    fun mapper(client: AmazonDynamoDB): DynamoDBTableMapper<DynamoCat, Int, String> = DynamoDBMapper(
            client,
            DynamoDBMapperConfig.Builder()
                    .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride(tableName))
                    .build()
    ).newTableMapper(DynamoCat::class.java)
}