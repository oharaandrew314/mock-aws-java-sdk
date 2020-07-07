package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.*
import org.assertj.core.api.Assertions.*

object TestUtils {

    fun ResourceNotFoundException.assertIsNotFound() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("Requested resource not found")
        assertThat(errorCode).isEqualTo("ResourceNotFoundException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun ResourceInUseException.assertTableInUse(tableName: String) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("Table already exists: $tableName")
        assertThat(errorCode).isEqualTo("ResourceInUseException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonDynamoDBException.assertIsMissingKey(key: AttributeDefinition) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("One or more parameter values were invalid: Missing the key ${key.attributeName} in the item")
        assertThat(errorCode).isEqualTo("ValidationException")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonDynamoDBException.assertIsMismatchedKey(key: AttributeDefinition, actual: ScalarAttributeType) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("One or more parameter values were invalid: Type mismatch for key ${key.attributeName} expected: ${key.attributeType} actual: $actual")
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