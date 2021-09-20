package io.andrewohara.awsmock.dynamodb.backend

typealias ItemCondition = (MockDynamoItem) -> Boolean
fun ItemCondition.inv(): ItemCondition = { invoke(it).not() }

typealias ValueCondition = (MockDynamoValue) -> Boolean
fun ValueCondition.forAttribute(name: String, ifMissing: Boolean = false): ItemCondition = { item ->
    item[name]?.let { value -> invoke(value) } ?: ifMissing
}
fun ValueCondition.not(): ValueCondition = { invoke(it).not() }

object Conditions {
    fun eq(arg: MockDynamoValue): ValueCondition = { it == arg }
    fun lt(arg: MockDynamoValue): ValueCondition = { it < arg }
    fun le(arg: MockDynamoValue): ValueCondition = { it <= arg }
    fun gt(arg: MockDynamoValue): ValueCondition = { it > arg }
    fun ge(arg: MockDynamoValue): ValueCondition = { it >= arg }
    fun exists(name: String): ItemCondition = { it[name] != null }
    fun contains(arg: MockDynamoValue): ValueCondition = { arg in it }
    fun beginsWith(arg: MockDynamoValue): ValueCondition = { it.startsWith(arg) }
    fun between(args: ClosedRange<MockDynamoValue>): ValueCondition = { it in args }
    fun inside(args: Collection<MockDynamoValue>): ValueCondition = { it in args }
}