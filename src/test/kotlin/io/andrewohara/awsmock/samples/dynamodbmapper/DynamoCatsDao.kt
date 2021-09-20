package io.andrewohara.awsmock.samples.dynamodbmapper

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTableMapper

class DynamoCatsDao(private val mapper: DynamoDBTableMapper<DynamoCat, Int, String>) {

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