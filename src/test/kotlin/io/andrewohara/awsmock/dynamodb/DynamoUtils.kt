package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoTable
import io.andrewohara.awsmock.dynamodb.v1.DynamoCat

object DynamoUtils {

    fun <T, H, R> MockDynamoBackend.createTable(type: Class<T>, name: String = type.simpleName): MockDynamoTable {
        val client = MockDynamoDbV1(this)

        val mapper = DynamoDBMapper(
            client,
            DynamoDBMapperConfig.Builder()
                .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride(name))
                .build()
        ).newTableMapper<T, H, R>(type)

        val result = mapper.createTable(ProvisionedThroughput(1, 1))
        return getTable(result.tableName)
    }

    fun MockDynamoBackend.createOwnersTable() = createTable<DynamoOwner, Int, Unit>(DynamoOwner::class.java, "owners")
    fun MockDynamoBackend.createCatsTable() = createTable<DynamoCat, Int, String>(DynamoCat::class.java, "cats")
}