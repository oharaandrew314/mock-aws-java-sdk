package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createOwnersTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class V2DescribeTableTest {

    private val clock = Clock.fixed(Instant.parse("2021-09-18T12:00:00Z"), ZoneOffset.UTC)
    private val backend = MockDynamoBackend(clock)
    private val client = MockDynamoDbV2(backend)
    private val cats = backend.createCatsTable()
    private val owners = backend.createOwnersTable()

    @Test
    fun `describe missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.describeTable {
                it.tableName("missingTable")
            }
        }
    }

    @Test
    fun `describe owners table with 0 items`() {
        client.describeTable { it.tableName(owners.name) } shouldBe DescribeTableResponse.builder()
            .table(TableDescription.builder()
                .tableArn(owners.arn)
                .tableName(owners.name)
                .creationDateTime(clock.instant())
                .tableStatus(TableStatus.ACTIVE)
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName("ownerId")
                        .attributeType(ScalarAttributeType.N)
                        .build()
                )
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName("ownerId")
                        .keyType(KeyType.HASH)
                        .build()
                )
                .itemCount(0)
                .globalSecondaryIndexes(emptyList())
                .localSecondaryIndexes(emptyList())
                .build()
            )
            .build()
    }

    @Test
    fun `describe cats table with 1 item`() {
        cats.save(DynamoFixtures.toggles)

        client.describeTable { it.tableName(cats.name) } shouldBe DescribeTableResponse.builder()
            .table(TableDescription.builder()
                .tableArn(cats.arn)
                .tableName(cats.name)
                .tableStatus(TableStatus.ACTIVE)
                .creationDateTime(clock.instant())
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName("ownerId").attributeType(ScalarAttributeType.N).build(),
                    AttributeDefinition.builder().attributeName("name").attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName("gender").attributeType(ScalarAttributeType.S).build()
                )
                .keySchema(
                    KeySchemaElement.builder().attributeName("ownerId").keyType(KeyType.HASH).build(),
                    KeySchemaElement.builder().attributeName("name").keyType(KeyType.RANGE).build()
                )
                .itemCount(1)
                .globalSecondaryIndexes(
                    GlobalSecondaryIndexDescription.builder()
                        .indexName("names")
                        .indexArn("${cats.arn}/index/names")
                        .itemCount(1)
                        .indexStatus(IndexStatus.ACTIVE)
                        .keySchema(
                            KeySchemaElement.builder().attributeName("name").keyType(KeyType.HASH).build()
                        )
                        .build()
                )
                .localSecondaryIndexes(
                    LocalSecondaryIndexDescription.builder()
                        .indexName("genders")
                        .indexArn("${cats.arn}/index/genders")
                        .itemCount(1)
                        .keySchema(
                            KeySchemaElement.builder().attributeName("ownerId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("gender").keyType(KeyType.RANGE).build()
                        )
                        .build()
                )
                .build()
            )
            .build()
    }
}