package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement

class MockTable(
        val name: String,
        private val hashKeyType: KeySchemaElement,
        private val rangeKeyType: KeySchemaElement?
) {
    private val hashes = mutableMapOf<AttributeValue, Hash>()


    fun save(item: MockItem) {
        val hashKey = item[hashKeyType.attributeName]!!  // TODO handle error
        val rangeKey = item.getRangeKey()

        val hash = ensureHash(hashKey)
        hash[rangeKey] = item
    }

    fun get(key: MockItem): MockItem? {
        val hashKey = key[hashKeyType.attributeName]!!  // TODO handle error
        val hash = hashes[hashKey] ?: return null
        return hash[key.getRangeKey()]
    }

    fun delete(key: MockItem): MockItem? {
        val hashKey = key[hashKeyType.attributeName]!!  // TODO handle error
        val hash = hashes[hashKey] ?: return null

        val item = hash[key.getRangeKey()] ?: return null
        hash.remove(key.getRangeKey())
        return item
    }

    fun scan(filter: Map<String, Condition>?): Collection<MockItem> {
        return hashes.flatMap { it.value.query(filter) }
    }

    fun query(keys: Map<String, Condition>, filter: Map<String, Condition>?, scanIndexForward: Boolean): List<MockItem> {
        val condition = keys.getValue(hashKeyType.attributeName)  //FIXME do the keyConditions always pertain only to the hash key?

        val items = hashes
                .filter { it.key.compareWith(condition) }
                .flatMap { it.value.query(filter) }

        if (rangeKeyType == null)   return items

        return items.sortedWith(MockItemComparator(rangeKeyType, !scanIndexForward))
    }

    private fun ensureHash(hashKey: AttributeValue): Hash {
        return if (hashes.containsKey(hashKey)) {
            hashes.getValue(hashKey)
        } else {
            val hash = Hash()
            hashes[hashKey] = hash
            hash
        }
    }

    private fun MockItem.getRangeKey(): AttributeValue? {
        if (rangeKeyType == null)   return null

        return getValue(rangeKeyType.attributeName)
    }
}

typealias Hash = HashMap<AttributeValue?, MockItem>

fun Hash.query(filter: Map<String, Condition>?) = values
        .filter { item ->
            filter == null || filter.all { (key, condition) -> item[key].compareWith(condition) }
        }