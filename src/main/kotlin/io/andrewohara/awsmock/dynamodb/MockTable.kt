package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement

class MockTable(
        val name: String,
        private val hashKeyType: KeySchemaElement,
        private val rangeKeyType: KeySchemaElement?
) {
    private val items = mutableListOf<MockItem>()


    fun save(item: MockItem) {
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
        val hashKeyCondition = keys.getValue(hashKeyType.attributeName)  //FIXME do the keyConditions always pertain only to the hash key?

        val filtered = items
                .filter {
                    val hashKey = it.hashKey()
                    val match = hashKey.compareWith(hashKeyCondition)
                    match
                } // query on hash key
                .filter(filter)

        if (rangeKeyType == null)   return filtered

        return filtered.sortedWith(MockItemComparator(rangeKeyType, !scanIndexForward))
    }

    private fun MockItem.hashKey(): AttributeValue = getValue(hashKeyType.attributeName)

    private fun MockItem.rangeKey(): AttributeValue? {
        if (rangeKeyType == null)   return null

        return getValue(rangeKeyType.attributeName)
    }

    private fun List<MockItem>.filter(filter: Map<String, Condition>?): List<MockItem> {
        return if (filter == null) {
            this
        } else {
            filter { item -> filter.all { (key, condition) -> item[key].compareWith(condition) } }
        }
    }
}