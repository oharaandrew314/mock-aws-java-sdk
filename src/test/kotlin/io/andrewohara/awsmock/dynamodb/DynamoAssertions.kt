package io.andrewohara.awsmock.dynamodb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException
import org.assertj.core.api.Assertions

object DynamoAssertions {

    fun ResourceNotFoundException.assertIsNotFound() {
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        Assertions.assertThat(errorMessage).isEqualTo("Requested resource not found")
        Assertions.assertThat(errorCode).isEqualTo("ResourceNotFoundException")
        Assertions.assertThat(statusCode).isEqualTo(400)
    }

    fun ResourceInUseException.assertTableInUse(tableName: String) {
        Assertions.assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        Assertions.assertThat(errorMessage).isEqualTo("Table already exists: $tableName")
        Assertions.assertThat(errorCode).isEqualTo("ResourceInUseException")
        Assertions.assertThat(statusCode).isEqualTo(400)
    }
}