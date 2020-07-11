package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.*
import java.util.*

data class MockTable(
        private val description: TableDescription,
        private val hashKeyDef: AttributeDefinition,
        private val rangeKeyDef: AttributeDefinition?
) {
    val name: String = description.tableName
    val items = mutableListOf<MockItem>()

    fun save(item: MockItem) {

        val hashKey = item.hashKey() ?: throw createMissingKeyException(hashKeyDef)
        if (hashKeyDef.type() != hashKey.dataType()) throw createMismatchedKeyException(hashKeyDef, hashKey.dataType())

        if (rangeKeyDef != null) {
            val rangeKey = item.rangeKey() ?: throw createMissingKeyException(rangeKeyDef)
            if (rangeKeyDef.type() != rangeKey.dataType()) throw createMismatchedKeyException(rangeKeyDef, rangeKey.dataType())
        }

        delete(item)
        items.add(item)
    }

    fun description() = description.withItemCount(items.size.toLong())

    fun get(key: MockItem): MockItem? {
        return items.firstOrNull { it.hashKey() == key.hashKey() && it.rangeKey() == key.rangeKey() }
    }

    fun delete(key: MockItem): MockItem? {
        val item = get(key) ?: return null
        items.remove(item)
        return item
    }

    fun scan(filter: Map<String, Condition>?): Collection<MockItem> {
        if (filter == null) return items

        return items.filter(filter)
    }

    fun query(keys: Map<String, Condition>, filter: Map<String, Condition>, scanIndexForward: Boolean): List<MockItem> {
        val hashKeyCondition = keys.getValue(hashKeyDef.attributeName)  //FIXME do the keyConditions always pertain only to the hash key?

        val filtered = items
                .filter { it.hashKey().compareWith(hashKeyCondition) } // query on hash key
                .filter(filter) // filter on rest of conditions

        if (rangeKeyDef == null)   return filtered

        return filtered.sortedWith(MockItemComparator(rangeKeyDef, !scanIndexForward))
    }

    private fun MockItem.hashKey(): AttributeValue? = get(hashKeyDef.attributeName)

    private fun MockItem.rangeKey(): AttributeValue? {
        if (rangeKeyDef == null)   return null

        return get(rangeKeyDef.attributeName)
    }

    private fun List<MockItem>.filter(filter: Map<String, Condition>): List<MockItem> {
        return filter { item -> filter.all { (key, condition) -> item[key].compareWith(condition) } }
    }

    private fun createMissingKeyException(key: AttributeDefinition) = AmazonDynamoDBException("One or more parameter values were invalid: Missing the key ${key.attributeName} in the item").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationException"
        statusCode = 400
    }

    private fun createMismatchedKeyException(key: AttributeDefinition, actual: ScalarAttributeType) = AmazonDynamoDBException("One or more parameter values were invalid: Type mismatch for key ${key.attributeName} expected: ${key.attributeType} actual: $actual").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationException"
        statusCode = 400
    }
}

private fun AttributeDefinition.type() = ScalarAttributeType.fromValue(attributeType)