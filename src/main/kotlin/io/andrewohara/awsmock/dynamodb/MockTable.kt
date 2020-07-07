package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.*
import java.util.*

class MockTable(
        val name: String,
        private val hashKeyDef: KeySchemaElement,
        private val hashKeyType: ScalarAttributeType,
        private val rangeKeyDef: KeySchemaElement?,
        private val rangeKeyType: ScalarAttributeType?
) {
    private val items = mutableListOf<MockItem>()

    fun save(item: MockItem) {
        val hashKey = item.hashKey() ?: throw createMissingKeyException(hashKeyDef)
        if (hashKeyType != hashKey.type()) throw createMismatchedKeyException(hashKeyDef.attributeName, hashKeyType, hashKey.type())

        if (rangeKeyDef != null) {
            val rangeKey = item.rangeKey() ?: throw createMissingKeyException(rangeKeyDef)
            if (rangeKeyType != rangeKey.type()) throw createMismatchedKeyException(rangeKeyDef.attributeName, rangeKeyType!!, rangeKey.type())
        }

        delete(item)
        items.add(item)
    }

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

    fun query(keys: Map<String, Condition>, filter: Map<String, Condition>?, scanIndexForward: Boolean): List<MockItem> {
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

    private fun List<MockItem>.filter(filter: Map<String, Condition>?): List<MockItem> {
        return if (filter == null) {
            this
        } else {
            filter { item -> filter.all { (key, condition) -> item[key].compareWith(condition) } }
        }
    }

    private fun createMissingKeyException(key: KeySchemaElement) = AmazonDynamoDBException("One or more parameter values were invalid: Missing the key ${key.attributeName} in the item").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationException"
        statusCode = 400
    }

    private fun createMismatchedKeyException(key: String, expected: ScalarAttributeType, actual: ScalarAttributeType) = AmazonDynamoDBException("One or more parameter values were invalid: Type mismatch for key $key expected: $expected actual: $actual").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationException"
        statusCode = 400
    }
}