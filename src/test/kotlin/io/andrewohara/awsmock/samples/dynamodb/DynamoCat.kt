package io.andrewohara.awsmock.samples.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey

@DynamoDBDocument
data class DynamoCat(
        @DynamoDBHashKey var ownerId: Int? = null,
        @DynamoDBRangeKey var catName: String? = null,

        var gender: String? = null
)