package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType

object V1Fixtures {
    val parents = mapOf(
        "ownerId" to AttributeValue().withN("1"),
        "name" to AttributeValue("Parents"),
        "pets" to AttributeValue().withN("2")
    )

    val meOwnerId: AttributeValue = AttributeValue().withN("2")
    val meKey = mapOf("ownerId" to meOwnerId)
    val me = meKey + mapOf(
        "name" to AttributeValue("Me"),
        "pets" to AttributeValue().withN("1")
    )

    val ownerIdAttribute = AttributeDefinition("ownerId", ScalarAttributeType.N)
    val nameAttribute = AttributeDefinition("name", ScalarAttributeType.S)

    val parentsOwnerId: AttributeValue = AttributeValue().withN("1")

    val smokeyKey = mapOf(
        "ownerId" to parentsOwnerId,
        "name" to AttributeValue("Smokey")
    )

    val smokey = smokeyKey + mapOf(
        "gender" to AttributeValue("female"),
        "features" to AttributeValue().withSS("grey", "active"),
        "visitDates" to AttributeValue().withNS("1337")
    )

    val bandit = mapOf(
        "ownerId" to parentsOwnerId,
        "name" to AttributeValue("Bandit"),
        "gender" to AttributeValue("male"),
        "features" to AttributeValue().withSS("grey", "lazy"),
        "visitDates" to AttributeValue().withNS("1337")
    )

    val togglesKey = mapOf(
        "ownerId" to meOwnerId,
        "name" to AttributeValue("Toggles")
    )

    val toggles = togglesKey + mapOf(
        "gender" to AttributeValue("female"),
        "features" to AttributeValue().withSS("brown", "old", "lazy"),
        "visitDates" to AttributeValue().withNS("1337", "9001")
    )
}