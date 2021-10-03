package io.andrewohara.awsmock.dynamodb.v2

import io.andrewohara.awsmock.dynamodb.DynamoFixtures
import io.andrewohara.awsmock.dynamodb.DynamoUtils.createCatsTable
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2
import io.andrewohara.awsmock.dynamodb.MockDynamoDbV2.Companion.toV2
import io.andrewohara.awsmock.dynamodb.backend.MockDynamoBackend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.*

class V2QueryTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val table = backend.createCatsTable().apply {
        save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)
    }

    @Test
    fun `query missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.query {
                it.tableName("missingTable")
                it.keyConditions(mapOf(
                    "ownerId" to Condition.builder()
                        .comparisonOperator(ComparisonOperator.EQ)
                        .attributeValueList(
                            AttributeValue.builder().n("1").build()
                        )
                        .build()
                ))
            }
        }
    }

    @Test
    fun `query only`() {
        client.query {
            it.tableName(table.name)
            it.keyConditions(mapOf(
                "ownerId" to Condition.builder()
                    .comparisonOperator(ComparisonOperator.EQ)
                    .attributeValueList(
                        AttributeValue.builder().n("1").build()
                    )
                    .build()
            ))
            it.scanIndexForward(false)
        } shouldBe QueryResponse.builder()
            .count(2)
            .items(DynamoFixtures.smokey.toV2(), DynamoFixtures.bandit.toV2())
            .build()
    }

    @Test
    fun `query with filter`() {
        client.query {
            it.tableName(table.name)
            it.keyConditions(mapOf(
                "ownerId" to Condition.builder()
                    .comparisonOperator(ComparisonOperator.EQ)
                    .attributeValueList(
                        AttributeValue.builder().n("1").build()
                    )
                    .build()
            ))
            it.filterExpression("gender = :gender")
            it.expressionAttributeValues(mapOf(
                ":gender" to AttributeValue.builder().s("female").build()
            ))
        } shouldBe QueryResponse.builder()
            .count(1)
            .items(DynamoFixtures.smokey.toV2())
            .build()
    }

    @Test
    fun `query by missing index`() {
        val exception = shouldThrow<DynamoDbException> {
            client.query {
                it.tableName(table.name)
                it.indexName("foos")
                it.keyConditions(mapOf(
                    "gender" to Condition.builder()
                        .comparisonOperator(ComparisonOperator.EQ)
                        .attributeValueList(
                            AttributeValue.builder().s("female").build()
                        )
                        .build()
                ))
            }
        }

        exception.awsErrorDetails().errorMessage() shouldBe "The table does not have the specified index: foos"
    }
}