package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.assertj.core.api.Assertions.*

object TestUtils {

    fun AmazonDynamoDBException.assertIsMissingKey(key: AttributeDefinition) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("One or more parameter values were invalid: Missing the key ${key.attributeName} in the item")
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonDynamoDBException.assertIsMismatchedKey() {
        errorType shouldBe AmazonServiceException.ErrorType.Client
        errorMessage.shouldContain("One or more parameter values were invalid")
        errorCode shouldBe "ValidationException"
        statusCode shouldBe 400
    }

    fun AmazonDynamoDBException.assertIsInvalidParameter() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).startsWith("One or more parameter values were invalid")
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonDynamoDBException.assertIsMissingIndex(indexName: String) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("The table does not have the specified index: $indexName")
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun Condition.eq(value: Int): Condition = this
            .withComparisonOperator(ComparisonOperator.EQ)
            .withAttributeValueList(AttributeValue().withN(value.toString()))

    fun Condition.eq(value: String): Condition = this
            .withComparisonOperator(ComparisonOperator.EQ)
            .withAttributeValueList(AttributeValue(value))
}