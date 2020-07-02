package io.andrewohara.awsmock.sqs

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sqs.model.*
import org.assertj.core.api.Assertions.assertThat

object SQSAssertions {

    fun AmazonSQSException.assertIsQueueNameAlreadyExists() {
        assertThat(this).isInstanceOf(QueueNameExistsException::class.java)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorMessage).isEqualTo("A queue already exists with the same name")
        assertThat(errorCode).isEqualTo("QueueAlreadyExists")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonSQSException.assertIsInvalidParameter() {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("InvalidParameterValue")
        assertThat(statusCode).isEqualTo(400)
    }

    fun AmazonSQSException.assertIsQueueDoesNotExist() {
        assertThat(this).isInstanceOf(QueueDoesNotExistException::class.java)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("AWS.SimpleQueueService.NonExistentQueue")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorMessage).isEqualTo("The specified queue does not exist for this wsdl version")
    }

    fun <T> AmazonSQSException.assertIsEmptyBatch(entryType: Class<T>) {
        assertThat(this).isInstanceOf(EmptyBatchRequestException::class.java)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("AWS.SimpleQueueService.EmptyBatchRequest")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorMessage).isEqualTo("There should be at least one ${entryType.simpleName} in the request")
    }

    fun AmazonSQSException.assertIsInvalidReceiptHandle() {
        assertThat(this).isInstanceOf(ReceiptHandleIsInvalidException::class.java)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("ReceiptHandleIsInvalid")
        assertThat(statusCode).isEqualTo(404)
        assertThat(errorMessage).isEqualTo("The input receipt handle is invalid")
    }

    fun AmazonSQSException.assertIsInvalidVisibilityTimeout(value: Int) {
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("InvalidParameterValue")
        assertThat(statusCode).isEqualTo(400)
        assertThat(errorMessage).isEqualTo("Value $value for parameter VisibilityTimeout is invalid. Reason: VisibilityTimeout must be an integer between 0 and 43200")
    }

    fun AmazonSQSException.assertIsInvalidReceiptHandleForQueue(handle: String) {
        assertThat(this).isInstanceOf(ReceiptHandleIsInvalidException::class.java)
        assertThat(errorType).isEqualTo(AmazonServiceException.ErrorType.Client)
        assertThat(errorCode).isEqualTo("ReceiptHandleIsInvalid")
        assertThat(statusCode).isEqualTo(404)
        assertThat(errorMessage).isEqualTo("The receipt handle \"$handle\" is not valid for this queue")
    }
}