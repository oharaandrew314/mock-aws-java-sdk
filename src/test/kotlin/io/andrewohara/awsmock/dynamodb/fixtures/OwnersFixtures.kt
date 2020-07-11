package io.andrewohara.awsmock.dynamodb.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.TestUtils.attributeValue

object OwnersFixtures {

    const val tableName = "owners"

    val parents = mapOf("ownerId" to attributeValue(1), "name" to AttributeValue("Parents"))
    val me = mapOf("ownerId" to attributeValue(2), "name" to AttributeValue("Me"))

    fun createTable(client: AmazonDynamoDB) {
        mapper(client).createTable(ProvisionedThroughput(1, 1))
    }

    fun mapper(client: AmazonDynamoDB): DynamoDBTableMapper<DynamoOwner, Int, String> = DynamoDBMapper(
            client,
            DynamoDBMapperConfig.Builder()
                    .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride(tableName))
                    .build()
    ).newTableMapper(DynamoOwner::class.java)
}