package io.andrewohara.awsmock.dynamodb.fixtures

import com.amazonaws.services.dynamodbv2.datamodeling.*

@DynamoDBTable(tableName = "cats")
data class DynamoCat(
    @DynamoDBHashKey var ownerId: Int? = null,
    @DynamoDBRangeKey var name: String? = null,

    var gender: String? = null
)