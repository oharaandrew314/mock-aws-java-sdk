package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement

class MockTable(
        val name: String,
        private val hashKeyType: KeySchemaElement,
        private val rangeKeyType: KeySchemaElement?
) {
    private val items = mutableMapOf<Any, HashItems>()


    fun save(item: MockItem) {
        val hashKey = item[hashKeyType.attributeName]!!  // TODO handle error
        val rangeKey = item.getRangeKey()

        val hash = ensureHash(hashKey)
        hash[rangeKey] = item
    }

    fun get(key: MockItem): MockItem? {
        val hashKey = key[hashKeyType.attributeName]!!  // TODO handle error
        val hash = items[hashKey] ?: return null
        return hash[key.getRangeKey()]
    }

    fun delete(key: MockItem): MockItem? {
        val hashKey = key[hashKeyType.attributeName]!!  // TODO handle error
        val hash = items[hashKey] ?: return null

        val item = hash[key.getRangeKey()] ?: return null
        hash.remove(key.getRangeKey())
        return item
    }

    fun scan(filter: Map<String, Condition>?): List<MockItem> {
        return items
                .flatMap { it.value.values }
                .filter { item ->
                    filter == null || filter.all { (key, condition) -> item[key].compareWith(condition) }
                }
    }

    private fun ensureHash(hashKey: AttributeValue): HashItems {
        return if (items.containsKey(hashKey)) {
            items.getValue(hashKey)
        } else {
            val hash = HashItems()
            items[hashKey] = hash
            hash
        }
    }

    private fun MockItem.getRangeKey(): AttributeValue? {
        if (rangeKeyType == null)   return null

        return getValue(rangeKeyType.attributeName)
    }
}

typealias HashItems = HashMap<AttributeValue?, MockItem>
