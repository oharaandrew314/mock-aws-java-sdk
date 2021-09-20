package io.andrewohara.awsmock.dynamodb

import io.andrewohara.awsmock.dynamodb.backend.MockDynamoItem
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoValue

object DynamoFixtures {

    val meOwnerId = MockDynamoValue(2)
    val meKey = MockDynamoItem(
        "ownerId" to meOwnerId,
    )
    val me = MockDynamoItem(
        *meKey.attributes.map { it.key to it.value }.toTypedArray(),
        "name" to MockDynamoValue(s = "Me"),
        "pets" to MockDynamoValue(1)
    )

    val parentsOwnerId = MockDynamoValue(n = 1)
    val parents = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockDynamoValue(s = "Parents"),
        "pets" to MockDynamoValue(2)
    )

    val smokey = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockDynamoValue(s = "Smokey"),
        "gender" to MockDynamoValue(s = "female"),
        "features" to MockDynamoValue(ss = setOf("grey", "active")),
        "visitDates" to MockDynamoValue(ns = setOf(1337))
    )

    val bandit = MockDynamoItem(
        "ownerId" to parentsOwnerId,
        "name" to MockDynamoValue(s = "Bandit"),
        "gender" to MockDynamoValue(s = "male"),
        "features" to MockDynamoValue(ss = setOf("grey", "lazy")),
        "visitDates" to MockDynamoValue(ns = setOf(1337))
    )

    val togglesOwnerId = MockDynamoValue(n = 2)

    val togglesKey = MockDynamoItem(
        "ownerId" to togglesOwnerId,
        "name" to MockDynamoValue(s = "Toggles")
    )

    val toggles = MockDynamoItem(
        *togglesKey.attributes.map { it.key to it.value }.toTypedArray(),
        "gender" to MockDynamoValue(s = "female"),
        "features" to MockDynamoValue(ss = setOf("brown", "old", "lazy")),
        "visitDates" to MockDynamoValue(ns = setOf(1337, 9001))
    )
}