package io.andrewohara.awsmock.dynamodb.backend

import java.math.BigDecimal
import java.nio.ByteBuffer

data class MockDynamoValue(
    val s: String? = null,
    val n: BigDecimal? = null,
    val b: ByteBuffer? = null,
    val bool: Boolean? = null,

    val ss: Set<String>? = null,
    val ns: Set<BigDecimal>? = null,
    val bs: Set<ByteBuffer>? = null,

    val list: List<MockDynamoValue>? = null,
    val map: MockDynamoItem? = null
): Comparable<MockDynamoValue> {
    companion object {
        operator fun invoke(n: Number): MockDynamoValue = MockDynamoValue(n = BigDecimal(n.toString()))
        operator fun invoke(ns: Set<Number>): MockDynamoValue = MockDynamoValue(ns = ns.map { BigDecimal(it.toString()) }.toSet())
    }

    val type: MockDynamoAttribute.Type = when {
        s != null -> MockDynamoAttribute.Type.String
        n != null -> MockDynamoAttribute.Type.Number
        b != null -> MockDynamoAttribute.Type.Binary
        bool != null -> MockDynamoAttribute.Type.Boolean

        ss != null -> MockDynamoAttribute.Type.StringSet
        ns != null -> MockDynamoAttribute.Type.NumberSet
        bs != null -> MockDynamoAttribute.Type.BinarySet

        list != null -> MockDynamoAttribute.Type.List
        map != null -> MockDynamoAttribute.Type.Map

        else -> MockDynamoAttribute.Type.Null
    }

    override fun compareTo(other: MockDynamoValue) = when {
        s != null && other.s != null -> s.compareTo(other.s)
        n != null && other.n != null -> n.compareTo(other.n)
        b != null && other.b != null -> b.compareTo(other.b)
        else -> 0
    }

    operator fun contains(other: MockDynamoValue) = when {
        s != null && other.s != null -> other.s in s
        ss != null && other.s != null -> other.s in ss
        ns != null && other.n != null -> other.n in ns
        bs != null && other.b != null -> other.b in bs
        b != null && other.b != null -> false // TODO implement
        else -> false
    }

    operator fun rangeTo(other: MockDynamoValue) = object: ClosedRange<MockDynamoValue> {
        override val endInclusive = other
        override val start = this@MockDynamoValue
    }

    fun startsWith(other: MockDynamoValue) = when {
        s != null && other.s != null -> s.startsWith(other.s)
        b != null && other.b != null -> false // TODO implement
        else -> false
    }
}