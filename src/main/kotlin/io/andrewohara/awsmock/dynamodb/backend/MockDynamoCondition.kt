package io.andrewohara.awsmock.dynamodb.backend

typealias ItemCondition = (MockDynamoItem) -> Boolean
fun ItemCondition.inv(): ItemCondition = { invoke(it).not() }

typealias ValueCondition = (MockValue) -> Boolean
fun ValueCondition.forAttribute(name: String, ifMissing: Boolean = false): ItemCondition = { item ->
    item[name]?.let { value -> invoke(value) } ?: ifMissing
}
fun ValueCondition.not(): ValueCondition = { invoke(it).not() }

object Conditions {
    fun eq(arg: MockValue): ValueCondition = { it == arg }
    fun lt(arg: MockValue): ValueCondition = { it < arg }
    fun le(arg: MockValue): ValueCondition = { it <= arg }
    fun gt(arg: MockValue): ValueCondition = { it > arg }
    fun ge(arg: MockValue): ValueCondition = { it >= arg }
    fun exists(name: String): ItemCondition = { it[name] != null }
    fun contains(arg: MockValue): ValueCondition = { arg in it }
    fun beginsWith(arg: MockValue): ValueCondition = { it.startsWith(arg) }
    fun between(args: ClosedRange<MockValue>): ValueCondition = { it in args }
    fun inside(args: Collection<MockValue>): ValueCondition = { it in args }
}