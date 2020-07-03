package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput

@DynamoDBTable(tableName = "cats")
data class DynamoCat(
    @DynamoDBHashKey var ownerId: Int? = null,
    @DynamoDBRangeKey var catName: String? = null,

    var gender: String? = null
) {
    companion object {
        const val tableName = "cats"
        fun mapper(ddb: AmazonDynamoDB): DynamoDBTableMapper<DynamoCat, Int, String> = DynamoDBMapper(ddb).newTableMapper<DynamoCat, Int, String>(DynamoCat::class.java)
        fun createTable(ddb: AmazonDynamoDB) {
            mapper(ddb).createTable(ProvisionedThroughput(1, 1))
        }
    }
}