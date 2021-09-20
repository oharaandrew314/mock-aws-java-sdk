package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class V1DescribeTableTest {

    private val clock = Clock.fixed(Instant.parse("2021-09-18T12:00:00Z"), ZoneOffset.UTC)
    private val backend = MockDynamoBackend(clock)
    private val client = MockDynamoDbV1(backend)
    private val cats = backend.createCatsTable()
    private val owners = backend.createOwnersTable()

    @Test
    fun `describe missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.describeTable("missingTable")
        }
    }

    @Test
    fun `describe owners table with 0 items`() {
        client.describeTable(owners.name) shouldBe DescribeTableResult()
            .withTable(TableDescription()
                .withTableArn(owners.arn)
                .withTableName(owners.name)
                .withCreationDateTime(Date.from(clock.instant()))
                .withTableStatus(TableStatus.ACTIVE)
                .withAttributeDefinitions(
                    AttributeDefinition().withAttributeName("ownerId").withAttributeType(ScalarAttributeType.N)
                )
                .withKeySchema(
                    KeySchemaElement().withAttributeName("ownerId").withKeyType(KeyType.HASH)
                )
                .withItemCount(0)
                .withGlobalSecondaryIndexes(emptyList())
                .withLocalSecondaryIndexes(emptyList())
            )
    }

    @Test
    fun `describe cats table with 1 item`() {
        cats.save(DynamoFixtures.toggles)

        client.describeTable(cats.name) shouldBe DescribeTableResult()
            .withTable(TableDescription()
                .withTableArn(cats.arn)
                .withTableName(cats.name)
                .withTableStatus(TableStatus.ACTIVE)
                .withCreationDateTime(Date.from(clock.instant()))
                .withAttributeDefinitions(
                    AttributeDefinition().withAttributeName("ownerId").withAttributeType(ScalarAttributeType.N),
                    AttributeDefinition().withAttributeName("name").withAttributeType(ScalarAttributeType.S),
                    AttributeDefinition().withAttributeName("gender").withAttributeType(ScalarAttributeType.S)
                )
                .withKeySchema(
                    KeySchemaElement().withAttributeName("ownerId").withKeyType(KeyType.HASH),
                    KeySchemaElement().withAttributeName("name").withKeyType(KeyType.RANGE)
                )
                .withItemCount(1)
                .withGlobalSecondaryIndexes(
                    GlobalSecondaryIndexDescription()
                        .withIndexName("names")
                        .withIndexArn("${cats.arn}/index/names")
                        .withItemCount(1)
                        .withIndexStatus(IndexStatus.ACTIVE)
                        .withKeySchema(
                            KeySchemaElement().withAttributeName("name").withKeyType(KeyType.HASH)
                        )
                )
                .withLocalSecondaryIndexes(
                    LocalSecondaryIndexDescription()
                        .withIndexName("genders")
                        .withIndexArn("${cats.arn}/index/genders")
                        .withItemCount(1)
                        .withKeySchema(
                            KeySchemaElement().withAttributeName("ownerId").withKeyType(KeyType.HASH),
                            KeySchemaElement().withAttributeName("gender").withKeyType(KeyType.RANGE)
                        )
                )
            )
    }
}