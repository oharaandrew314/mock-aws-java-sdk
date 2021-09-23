package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class V2CreateTableUnitTest {

    private val clock = Clock.fixed(Instant.ofEpochSecond(9001), ZoneOffset.UTC)
    private val backend = MockDynamoBackend(clock)
    private val client = MockDynamoDbV2(backend)

    @Test
    fun `create table`() {
        createDoggosTable() shouldBe CreateTableResponse.builder()
            .tableDescription(TableDescription.builder()
                .tableName("doggos")
                .tableArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos")
                .itemCount(0)
                .tableStatus(TableStatus.ACTIVE)
                .creationDateTime(clock.instant())
                .attributeDefinitions(numAttr("ownerId"), stringAttr("doggoName"))
                .keySchema(hashkey("ownerId"), rangeKey("doggoName"))
                .localSecondaryIndexes(emptyList())
                .globalSecondaryIndexes(emptyList())
                .build()
            )
            .build()

        backend["doggos"].shouldNotBeNull()
    }

    @Test
    fun `create table that already exists`() {
        createDoggosTable()
        shouldThrow<ResourceInUseException> { createDoggosTable() }
    }

    @Test
    fun `create table where key schema doesn't have associated AttributeSchema`() {
        shouldThrow<DynamoDbException> {
            client.createTable {
                it.tableName("doggos")
                it.attributeDefinitions(numAttr("ownerId"), stringAttr("foo"))
                it.keySchema(hashkey("ownerId"), rangeKey("doggoName"))
            }
        }
    }

    @Test
    fun `create table with indexes`() {
        val request = CreateTableRequest
            .builder()
            .tableName("doggos")
            .attributeDefinitions(numAttr("ownerId"), stringAttr("name"), stringAttr("dob"))
            .keySchema(hashkey("ownerId"), hashkey("name"))
            .globalSecondaryIndexes(
                GlobalSecondaryIndex.builder()
                    .indexName("names")
                    .keySchema(hashkey("name"))
                    .build()
            )
            .localSecondaryIndexes(
                LocalSecondaryIndex.builder()
                    .indexName("dob")
                    .keySchema(hashkey("ownerId"), rangeKey("dob"))
                    .build()
            )
            .build()

        val result = client.createTable(request)

        result.tableDescription().globalSecondaryIndexes().shouldContainExactly(
            GlobalSecondaryIndexDescription.builder()
                .indexName("names")
                .indexArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos/index/names")
                .indexStatus(IndexStatus.ACTIVE)
                .itemCount(0)
                .keySchema(hashkey("name"))
                .build()
        )
        result.tableDescription().localSecondaryIndexes().shouldContainExactly(
            LocalSecondaryIndexDescription.builder()
                .indexName("dob")
                .indexArn("arn:aws:dynamodb-mock:ca-central-1:0123456789:table/doggos/index/dob")
                .itemCount(0)
                .keySchema(hashkey("ownerId"), rangeKey("dob"))
                .build()
        )
    }

    private fun createDoggosTable(): CreateTableResponse {
        return client.createTable {
            it.tableName("doggos")
            it.attributeDefinitions(numAttr("ownerId"), stringAttr("doggoName"))
            it.keySchema(hashkey("ownerId"), rangeKey("doggoName"))
        }
    }

    companion object {
        private fun stringAttr(name: String): AttributeDefinition = AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build()
        private fun numAttr(name: String) : AttributeDefinition= AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.N).build()
        private fun hashkey(name: String): KeySchemaElement = KeySchemaElement.builder().attributeName(name).keyType(KeyType.HASH).build()
        private fun rangeKey(name: String): KeySchemaElement = KeySchemaElement.builder().attributeName(name).keyType(KeyType.RANGE).build()
    }
}