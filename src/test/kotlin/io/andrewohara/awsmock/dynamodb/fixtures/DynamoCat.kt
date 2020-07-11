package io.andrewohara.awsmock.dynamodb.fixtures

import com.amazonaws.services.dynamodbv2.datamodeling.*

@DynamoDBTable(tableName = "cats")
data class DynamoCat(
        @DynamoDBHashKey
        var ownerId: Int? = null,

        @DynamoDBRangeKey
        @DynamoDBIndexHashKey(globalSecondaryIndexName = "names")
        var name: String? = null,

        @DynamoDBIndexRangeKey(localSecondaryIndexName = "genders")
        var gender: String? = null
)