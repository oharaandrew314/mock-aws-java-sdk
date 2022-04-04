package io.andrewohara.awsmock.dynamodb.backend

fun interface MockDynamoCondition2: (MockDynamoItem) -> Boolean {
    infix fun and(other: MockDynamoCondition2) = MockDynamoCondition2 { item -> this(item) && other(item) }
    infix fun or(other: MockDynamoCondition2) = MockDynamoCondition2 { item -> this(item) || other(item) }
    operator fun not() = MockDynamoCondition2 { item -> this(item).not() }

    companion object
}

private fun ifExists(attribute: String, condition: (MockDynamoValue) -> Boolean) = MockDynamoCondition2 { item ->
    val value = item[attribute]
    value != null && condition(value)
}

fun String.eq(arg: MockDynamoValue) = ifExists(this) { it == arg }
fun String.lt(arg: MockDynamoValue) = ifExists(this) { it < arg }
fun String.le(arg: MockDynamoValue) = ifExists(this) { it <= arg }
fun String.gt(arg: MockDynamoValue) = ifExists(this) { it > arg }
fun String.ge(arg: MockDynamoValue) = ifExists(this) { it >= arg }
fun String.exists() = ifExists(this) { true }
fun String.contains(arg: MockDynamoValue) = ifExists(this) { arg in it }
fun String.beginsWith(arg: MockDynamoValue) = ifExists(this) { it.startsWith(arg) }
fun String.between(arg1: MockDynamoValue, arg2: MockDynamoValue) = ifExists(this) { it in arg1..arg2 }
fun String.inside(args: Collection<MockDynamoValue>) = ifExists(this) { it in args }

fun MockDynamoCondition2.Companion.parseExpression(expression: String, vararg values: Pair<String, MockDynamoValue>): MockDynamoCondition2 {
    TODO()
//    ConditionExpressionBaseVisitor
}