package io.andrewohara.awsmock.dynamodb.backend

import java.lang.IllegalArgumentException

data class MockDynamoItem(val attributes: Map<String, MockValue>) {
    constructor(vararg attributes: Pair<String, MockValue>): this(attributes.toMap())
    /**
     * TODO this needs to be further implemented
     * e.g. for set types
     */
    fun withUpdates(updates: Map<String, MockDynamoUpdate>): MockDynamoItem {
        val attributes = this.attributes.toMutableMap()

        for ((key, value) in updates) {
            when(value.action) {
                MockDynamoUpdate.Type.Add -> {
                    val attr = attributes.getValue(key)

                    val current = attr.n ?: throw IllegalArgumentException("target attribute must be N") // TODO throw correct error
                    val increment = value.value?.n ?: throw IllegalArgumentException("update attribute doesn't match target attribute")  // TODO throw correct error

                    attributes[key] = MockValue(current + increment)
                }
                MockDynamoUpdate.Type.Put-> {
                    val update = value.value ?: throw IllegalArgumentException("no value to put") // TODO throw correct error
                    attributes[key] = update
                }
                MockDynamoUpdate.Type.Delete-> {
                    attributes.remove(key)
                }
            }
        }

        return MockDynamoItem(attributes)
    }

    fun plus(vararg values: Pair<String, MockValue>) = MockDynamoItem(
        *attributes.map { it.key to it.value }.toTypedArray(),
        *values
    )

    fun minus(vararg values: String) = MockDynamoItem(attributes.filterKeys { it !in values })

    operator fun get(key: MockDynamoAttribute) = attributes[key.name]
    operator fun get(key: String) = attributes[key]
}

data class MockDynamoUpdate(
    val action: Type,
    val value: MockValue?
) {
    enum class Type { Add, Put, Delete }
}