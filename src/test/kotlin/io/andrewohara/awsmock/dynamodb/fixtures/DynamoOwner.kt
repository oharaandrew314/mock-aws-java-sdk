package io.andrewohara.awsmock.dynamodb.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput

@DynamoDBDocument
data class DynamoOwner(
        @DynamoDBHashKey var ownerId: Int? = null,

        var name: String? = null,

        var pets: Int? = null
) {
    companion object {
        const val tableName = "cats"
        fun mapper(ddb: AmazonDynamoDB): DynamoDBTableMapper<DynamoCat, Int, String> = DynamoDBMapper(ddb).newTableMapper<DynamoCat, Int, String>(DynamoCat::class.java)
        fun createTable(ddb: AmazonDynamoDB) {
            mapper(ddb).createTable(ProvisionedThroughput(1, 1))
        }
    }
}