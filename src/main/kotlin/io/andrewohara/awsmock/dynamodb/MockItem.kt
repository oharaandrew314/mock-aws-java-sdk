package io.andrewohara.awsmock.dynamodb

import com.amazonaws.services.dynamodbv2.model.*
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator.*
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException

typealias MockItem = Map<String, AttributeValue>

/**
 * TODO this needs to be further implemented
 * e.g. for set types
 */
fun MockItem.update(updates: Map<String, AttributeValueUpdate>): MockItem {
    val toUpdate = toMutableMap()
    for ((key, value) in updates) {
        when(value.action) {
            "ADD" -> {
                val attr = toUpdate.getValue(key)
                if (attr.dataType() != ScalarAttributeType.N) throw IllegalArgumentException("target attribute must be N") // TODO throw correct error
                if (value.value.dataType() != attr.dataType()) throw IllegalArgumentException("update attribute doesn't match target attribute")  // TODO throw correct error
                attr.n = attr.n.toBigDecimal().plus(value.value.n.toBigDecimal()).toString()
            }
            "PUT" -> toUpdate[key] = value.value
            "DELETE" -> {
                toUpdate.remove(key)
            }
        }
    }
    return toUpdate
}

operator fun AttributeValue.compareTo(other: AttributeValue) = when {
    s != null -> s.compareTo(other.s)
    n != null -> n.toBigDecimal().compareTo(other.n.toBigDecimal())
    b != null -> b.compareTo(other.b)
    else -> throw IllegalArgumentException() // TODO throw correct error
}

operator fun AttributeValue.contains(other: AttributeValue) = when {
    s != null -> other.s in s
    b != null -> throw UnsupportedOperationException("yeah right;  You implement this if you want it that bad")
    ss != null -> other.s in ss
    ns != null -> other.n in ns
    else -> throw IllegalArgumentException() // TODO throw correct error
}

//operator fun AttributeValue.contains(others: Collection<AttributeValue>) = when {
//    s != null -> others.any { it.s == s }
//    n != null -> others.any { it.n == n }
//    b != null -> others.any { it.b == b }
//    else -> throw IllegalArgumentException() // TODO throw correct error
//}

fun AttributeValue.startsWith(other: AttributeValue) = when {
    s != null -> s.startsWith(other.s)
    b != null -> throw UnsupportedOperationException("yeah right;  You implement this if you want it that bad")
    else  -> throw IllegalArgumentException() // TODO throw correct error
}

fun AttributeValue?.compareWith(condition: Condition) = when(fromValue(condition.comparisonOperator)!!) {
    NOT_NULL     -> this != null
    NULL         -> this == null
    GT           -> this != null && this > condition.attributeValueList.first()
    GE           -> this != null && this >= condition.attributeValueList.first()
    LT           -> this != null && this < condition.attributeValueList.first()
    LE           -> this != null && this <= condition.attributeValueList.first()
    NE           -> this != condition.attributeValueList.firstOrNull()
    EQ           -> this == condition.attributeValueList.firstOrNull()
    CONTAINS     -> this != null && condition.attributeValueList.any { it in this }
    NOT_CONTAINS -> this != null && condition.attributeValueList.none { it in this }
    BEGINS_WITH  -> this != null && this.startsWith(condition.attributeValueList.first())
    IN           -> this != null && this in condition.attributeValueList
    BETWEEN      -> this != null && this >= condition.attributeValueList.first() && this <= condition.attributeValueList.last()
}

fun AttributeValue.dataType(): ScalarAttributeType = when {
    s != null -> ScalarAttributeType.S
    n != null -> ScalarAttributeType.N
    b != null -> ScalarAttributeType.B
    else -> throw IllegalArgumentException()
}

class MockItemComparator(private val rangeKey: AttributeDefinition, private val reverse: Boolean): Comparator<MockItem> {
    override fun compare(i1: MockItem, i2: MockItem): Int {
        val rangeKey1 = i1.getValue(rangeKey.attributeName)
        val rangeKey2 = i2.getValue(rangeKey.attributeName)

        return if (reverse) rangeKey2.compareTo(rangeKey1) else rangeKey1.compareTo(rangeKey2)
    }
}
