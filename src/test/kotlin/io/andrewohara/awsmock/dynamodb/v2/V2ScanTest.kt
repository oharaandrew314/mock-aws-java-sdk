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

class V2ScanTest {

    private val backend = MockDynamoBackend()
    private val client = MockDynamoDbV2(backend)
    private val cats = backend.createCatsTable()

    @Test
    fun `scan missing table`() {
        shouldThrow<ResourceNotFoundException> {
            client.scan {
                it.tableName("missingTable")
            }
        }
    }

    @Test
    fun `scan with conditions`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        client.scan {
            it.tableName(cats.name)
            it.scanFilter(mapOf(
                "gender" to Condition.builder()
                    .comparisonOperator(ComparisonOperator.EQ)
                    .attributeValueList(
                        AttributeValue.builder().s("male").build()
                    )
                    .build()
            ))
        } shouldBe ScanResponse.builder()
            .count(1)
            .items(DynamoFixtures.bandit.toV2())
            .build()
    }

    @Test
    fun `scan with filter expression`() {
        cats.save(DynamoFixtures.toggles, DynamoFixtures.smokey, DynamoFixtures.bandit)

        client.scan {
            it.tableName(cats.name)
            it.filterExpression("name = :name and gender = :gender")
            it.expressionAttributeValues(mapOf(
                ":name" to AttributeValue.builder().s("Smokey").build(),
                ":gender" to AttributeValue.builder().s("female").build()
            ))
        } shouldBe ScanResponse.builder()
            .count(1)
            .items(DynamoFixtures.smokey.toV2())
            .build()
    }
}