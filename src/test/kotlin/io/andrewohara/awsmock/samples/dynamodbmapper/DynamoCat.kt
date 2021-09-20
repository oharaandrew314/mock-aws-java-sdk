package io.andrewohara.awsmock.samples.dynamodbmapper

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.*

@DynamoDBTable(tableName = "cats")
data class DynamoCat(
    @DynamoDBHashKey var ownerId: Int? = null,
    @DynamoDBRangeKey var catName: String? = null,

    var gender: String? = null
) {
    companion object {
        fun mapper(client: AmazonDynamoDB): DynamoDBTableMapper<DynamoCat, Int, String> {
            return DynamoDBMapper(client).newTableMapper(DynamoCat::class.java)
        }
    }
}