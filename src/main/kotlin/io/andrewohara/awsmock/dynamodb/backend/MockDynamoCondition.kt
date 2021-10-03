package io.andrewohara.awsmock.dynamodb.backend

import java.lang.IllegalArgumentException

data class MockDynamoCondition(
    val operator: Operator,
    val arguments: List<MockDynamoValue>,
    val inverse: Boolean = false
) {
    operator fun invoke(name: String, item: MockDynamoItem): Boolean {
        val result = item[name]
            ?.let { operator(it, arguments) }
            ?: false
        return if (inverse) !result else result
    }

    operator fun not() = copy(inverse = !inverse)

    companion object {
        fun eq(arg: MockDynamoValue) = MockDynamoCondition(Operator.Eq, listOf(arg))
        fun lt(arg: MockDynamoValue) = MockDynamoCondition(Operator.Lt, listOf(arg))
        fun le(arg: MockDynamoValue) = MockDynamoCondition(Operator.Le, listOf(arg))
        fun gt(arg: MockDynamoValue) = MockDynamoCondition(Operator.Gt, listOf(arg))
        fun ge(arg: MockDynamoValue) = MockDynamoCondition(Operator.Ge, listOf(arg))
        fun exists() = MockDynamoCondition(Operator.Exists, emptyList())
        fun contains(arg: MockDynamoValue) = MockDynamoCondition(Operator.Contains, listOf(arg))
        fun beginsWith(arg: MockDynamoValue) = MockDynamoCondition(Operator.BeginsWith, listOf(arg))
        fun between(start: MockDynamoValue, end: MockDynamoValue) = MockDynamoCondition(Operator.Between, listOf(start, end))
        fun inside(args: Collection<MockDynamoValue>) = MockDynamoCondition(Operator.Inside, args.toList())
        fun inside(vararg args: MockDynamoValue) = inside(args.toList())

        private val conditionRegex = "([\\w.-]+) (=|<[=]?|>[=]?) (:[\\w.-]+)".toRegex()

        fun parseExpression(expression: String, vararg values: Pair<String, MockDynamoValue>) = parseExpression(expression, MockDynamoItem(values.toMap()))

        fun parseExpression(expression: String, values: MockDynamoItem): Map<String, MockDynamoCondition> {
            return conditionRegex.findAll(expression).map { match ->
                val (_, name, operator, valueName) = match.groupValues
                val value = values[valueName]!!

                val condition = when(operator) {
                    "=" -> eq(value)
                    "<" -> lt(value)
                    "<=" -> le(value)
                    ">" -> gt(value)
                    ">=" -> ge(value)
                    else -> throw IllegalArgumentException("Illegal operator: $operator")
                }

                name.replace("AMZN_MAPPED_", "") to condition
            }.toMap()
        }
    }
}

enum class Operator(private val condition: (MockDynamoValue, List<MockDynamoValue>) -> Boolean) {
    Eq({ value, args -> args.first() == value }),
    Lt({ value, args -> value < args.first() }),
    Le({ value, args -> value <= args.first() }),
    Ge({ value, args -> value >= args.first() }),
    Gt({ value, args -> value > args.first() }),
    Exists({ _, _ -> true }),
    Contains({ value, args -> args.first() in value }),
    BeginsWith({ value, args -> value.startsWith(args.first())}),
    Between({ value, args -> value in args.first()..args.last() }),
    Inside({ value, args -> value in args });

    operator fun invoke(value: MockDynamoValue, args: List<MockDynamoValue>) = condition(value, args)
}