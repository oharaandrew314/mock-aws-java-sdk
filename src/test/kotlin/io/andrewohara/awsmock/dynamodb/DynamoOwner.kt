package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey

@DynamoDBDocument
data class DynamoOwner(
    @DynamoDBHashKey
    var ownerId: Int? = null,

    var name: String? = null,
    var pets: Int? = null
)