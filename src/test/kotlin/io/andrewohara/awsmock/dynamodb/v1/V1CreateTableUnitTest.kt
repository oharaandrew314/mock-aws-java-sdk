package io.andrewohara.awsmock.dynamodb.v1

import com.amazonaws.services.dynamodbv2.model.*
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV1
import io.andrewohara.awsmock.dynamodb.TestUtils.assertIsInvalidParameter
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class V1CreateTableUnitTest {

    private val clock = Clock.fixed(Instant.ofEpochSecond(9001), ZoneOffset.UTC)
    private val backend = MockDynamoBackend(clock)
    private val client = MockDynamoDbV1(backend)

    @Test
    fun `create table`() {
        createDoggosTable() shouldBe CreateTableResult()
            .withTableDescription(
                TableDescription()
                    .withTableName("doggos")
                    .withTableArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos")
                    .withItemCount(0)
                    .withTableStatus(TableStatus.ACTIVE)
                    .withCreationDateTime(Date.from(clock.instant()))
                    .withAttributeDefinitions(
                        AttributeDefinition("ownerId", ScalarAttributeType.N),
                        AttributeDefinition("doggoName", ScalarAttributeType.S)
                    )
                    .withKeySchema(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("doggoName", KeyType.RANGE)
                    )
                    .withGlobalSecondaryIndexes(emptyList())
                    .withLocalSecondaryIndexes(emptyList())
            )

        backend["doggos"].shouldNotBeNull()
    }

    @Test
    fun `create table that already exists`() {
        createDoggosTable()
        shouldThrow<ResourceInUseException> { createDoggosTable() }
    }

    @Test
    fun `create table where key schema doesn't have associated AttributeSchema`() {
        shouldThrow<AmazonDynamoDBException> {
            client.createTable(
                listOf(
                    AttributeDefinition("ownerId", ScalarAttributeType.N),
                    AttributeDefinition("foo", ScalarAttributeType.S)
                ),
                "doggos",
                listOf(
                    KeySchemaElement("ownerId", KeyType.HASH),
                    KeySchemaElement("doggoName", KeyType.RANGE)
                ),
                ProvisionedThroughput(1, 1)
            )
        }.assertIsInvalidParameter()
    }

    @Test
    fun `create table with indexes`() {
        val request = CreateTableRequest()
            .withTableName("doggos")
            .withAttributeDefinitions(
                AttributeDefinition("ownerId", ScalarAttributeType.N),
                AttributeDefinition("name", ScalarAttributeType.S),
                AttributeDefinition("dob", ScalarAttributeType.S)
            )
            .withKeySchema(
                KeySchemaElement("ownerId", KeyType.HASH),
                KeySchemaElement("name", KeyType.RANGE)
            )
            .withGlobalSecondaryIndexes(
                GlobalSecondaryIndex()
                    .withIndexName("names")
                    .withKeySchema(KeySchemaElement("name", KeyType.HASH))
                    .withProjection(Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(ProvisionedThroughput(1, 1))
            )
            .withLocalSecondaryIndexes(
                LocalSecondaryIndex()
                    .withIndexName("dob")
                    .withKeySchema(
                        KeySchemaElement("ownerId", KeyType.HASH),
                        KeySchemaElement("dob", KeyType.RANGE)
                    )
                    .withProjection(Projection().withProjectionType(ProjectionType.ALL))
            )
            .withProvisionedThroughput(ProvisionedThroughput(1, 1))

        val result = client.createTable(request)

        result.tableDescription.globalSecondaryIndexes.shouldContainExactly(
            GlobalSecondaryIndexDescription()
                .withIndexName("names")
                .withIndexArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos/index/names")
                .withIndexStatus(IndexStatus.ACTIVE)
                .withItemCount(0)
                .withKeySchema(
                    KeySchemaElement("name", KeyType.HASH)
                )
        )
        result.tableDescription.localSecondaryIndexes.shouldContainExactly(
            LocalSecondaryIndexDescription()
                .withIndexName("dob")
                .withIndexArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos/index/dob")
                .withItemCount(0)
                .withKeySchema(
                    KeySchemaElement("ownerId", KeyType.HASH),
                    KeySchemaElement("dob", KeyType.RANGE)
                )
        )
    }

    private fun createDoggosTable(): CreateTableResult {
        return client.createTable(
            listOf(
                AttributeDefinition("ownerId", ScalarAttributeType.N),
                AttributeDefinition("doggoName", ScalarAttributeType.S)
            ),
            "doggos",
            listOf(
                KeySchemaElement("ownerId", KeyType.HASH),
                KeySchemaElement("doggoName", KeyType.RANGE)
            ),
            ProvisionedThroughput(1, 1)
        )
    }
}