package io.andrewohara.awsmock.dynamodb.backend

import io.andrewohara.awsmock.core.MockAwsException
import java.time.Instant
import kotlin.Comparator

class MockDynamoTable(
    val schema: MockDynamoSchema,
    val created: Instant,
    val globalIndices: Collection<MockDynamoSchema>,
    val localIndices: Collection<MockDynamoSchema>,
    val enforceIndices: Boolean = true
) {
    val name = schema.name
    val arn = "arn:aws:dynamodb-mock:ca-central-1:0123456789:table/$name"

    val items = mutableListOf<MockDynamoItem>()

    fun attributes() = (schema.attributes() + globalIndices.flatMap { it.attributes() } + localIndices.flatMap { it.attributes() }).toSet()

    fun save(vararg toSave: MockDynamoItem) {
        for (item in toSave) {
            schema.assertObeys(item)
        }

        for (item in toSave) {
            delete(item)
            items += item
        }
    }

    operator fun get(key: MockDynamoItem): MockDynamoItem? {
        return items
            .filter { it[schema.hashKey] == key[schema.hashKey] }
            .find { if (schema.rangeKey == null) true else it[schema.rangeKey] == key[schema.rangeKey] }
    }

    fun delete(key: MockDynamoItem): MockDynamoItem? {
        val item = get(key) ?: return null
        items.remove(item)
        return item
    }

    fun scan(vararg conditions: Pair<String, MockDynamoCondition>) = scan(conditions.toMap())

    fun scan(conditions: Map<String, MockDynamoCondition>): Set<MockDynamoItem> {
        return items.filter(conditions).toSet()
    }

    fun query(vararg conditions: Pair<String, MockDynamoCondition>) = query(conditions.toMap())

    fun query(conditions: Map<String, MockDynamoCondition>, scanIndexForward: Boolean = true, indexName: String? = null): List<MockDynamoItem> {
        val schema = when {
            indexName == null -> schema
            enforceIndices -> (globalIndices + localIndices).find { it.name == indexName } ?: throw missingIndex(indexName)
            else -> null
        }

        val filtered = items.filter(conditions)

        if (schema?.rangeKey == null) return filtered

        return filtered.sortedWith(MockItemComparator(schema.rangeKey, !scanIndexForward))
    }

    fun update(key: MockDynamoItem, updates: Map<String, MockDynamoUpdate>): MockDynamoItem? {
        val existing = get(key)
        if (existing != null) {
            val updated = existing.withUpdates(updates)
            save(updated)
            return updated
        }

        val item = key.withUpdates(updates)
        if (item == key) return null

        save(item)
        return item
    }

    private fun List<MockDynamoItem>.filter(conditions: Map<String, MockDynamoCondition>): List<MockDynamoItem> {
        return filter { item ->
            conditions.all { (name, condition) ->
                condition(name, item)
            }
        }
    }
}

class MockItemComparator(private val rangeKey: MockDynamoAttribute, private val reverse: Boolean): Comparator<MockDynamoItem> {
    override fun compare(item1: MockDynamoItem, item2: MockDynamoItem): Int {
        val rangeKey1 = item1[rangeKey] ?: throw validationFailed()
        val rangeKey2 = item2[rangeKey] ?: throw validationFailed()

        return if (reverse) rangeKey2.compareTo(rangeKey1) else rangeKey1.compareTo(rangeKey2)
    }
}

fun validationFailed() = MockAwsException(
    message = "One or more parameter values were invalid",
    errorCode = "ValidationException",
    statusCode = 400
)

private fun missingIndex(name: String) = MockAwsException(
    message = "The table does not have the specified index: $name",
    errorCode = "ValidationException",
    statusCode = 400
)