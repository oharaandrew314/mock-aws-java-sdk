package io.andrewohara.awsmock.samples.dynamodb

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper

class DynamoCatsDao(tableName: String, client: AmazonDynamoDB? = null) {

    val mapper: DynamoDBTableMapper<DynamoCat, Int, String> = DynamoDBMapper(
                    client ?: AmazonDynamoDBClientBuilder.defaultClient(),
                    DynamoDBMapperConfig.Builder()
                            .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride(tableName))
                            .build()
            )
            .newTableMapper(DynamoCat::class.java)

    fun list(ownerId: Int): Collection<DynamoCat> {
        val query = DynamoDBQueryExpression<DynamoCat>()
                .withHashKeyValues(DynamoCat(ownerId = ownerId))

        return mapper.query(query)
    }

    operator fun get(ownerId: Int, name: String): DynamoCat? {
        return mapper.load(ownerId, name)
    }

    fun save(cat: DynamoCat) {
        mapper.save(cat)
    }

    fun delete(cat: DynamoCat) {
        mapper.delete(cat)
    }
}