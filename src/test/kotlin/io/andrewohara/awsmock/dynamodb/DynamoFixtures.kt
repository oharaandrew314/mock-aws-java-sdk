package io.andrewohara.awsmock.dynamodb

import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockValue

object DynamoFixtures {

    val meOwnerId = MockValue(2)
    val meKey = MockDynamoItem(
        "ownerId" to meOwnerId,
    )
    val me = MockDynamoItem(
        *meKey.attributes.map { it.key to it.value }.toTypedArray(),
        "name" to MockValue(s = "Me"),
        "pets" to MockValue(1)
    )

    val parentsOwnerId = MockValue(n = 1)
    val parents = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockValue(s = "Parents"),
        "pets" to MockValue(2)
    )

    val smokey = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockValue(s = "Smokey"),
        "gender" to MockValue(s = "female"),
        "features" to MockValue(ss = setOf("grey", "active")),
        "visitDates" to MockValue(ns = setOf(1337))
    )

    val bandit = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockValue(s = "Bandit"),
        "gender" to MockValue(s = "male"),
        "features" to MockValue(ss = setOf("grey", "lazy")),
        "visitDates" to MockValue(ns = setOf(1337))
    )

    val togglesOwnerId = MockValue(n = 2)

    val togglesKey = MockDynamoItem(
        "ownerId" to togglesOwnerId,
        "name" to MockValue(s = "Toggles")
    )

    val toggles = MockDynamoItem(
        *togglesKey.attributes.map { it.key to it.value }.toTypedArray(),
        "gender" to MockValue(s = "female"),
        "features" to MockValue(ss = setOf("brown", "old", "lazy")),
        "visitDates" to MockValue(ns = setOf(1337, 9001))
    )
}