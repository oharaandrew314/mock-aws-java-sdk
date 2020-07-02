package io.andrewohara.awsmock.sqs

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sqs.model.*
import java.util.*

object SQSExceptions {
    fun createQueueExistsException(): QueueNameExistsException {
        return QueueNameExistsException("A queue already exists with the same name").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "QueueAlreadyExists"
            statusCode = 400
            requestId = UUID.randomUUID().toString()
        }
    }

    fun createInvalidParameterException(): AmazonSQSException {
        return AmazonSQSException("Value for parameter is invalid").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "InvalidParameterValue"
            statusCode = 400
            requestId = UUID.randomUUID().toString()
        }
    }

    fun createQueueDoesNotExistException(): QueueDoesNotExistException {
        return QueueDoesNotExistException("The specified queue does not exist for this wsdl version").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "AWS.SimpleQueueService.NonExistentQueue"
            statusCode = 400
            requestId = UUID.randomUUID().toString()
        }
    }

    fun <T> createEmptyBatchException(entryType: Class<T>): EmptyBatchRequestException {
        return EmptyBatchRequestException("There should be at least one ${entryType.simpleName} in the request").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "AWS.SimpleQueueService.EmptyBatchRequest"
            statusCode = 400
            requestId = UUID.randomUUID().toString()
        }
    }

    fun createInvalidReceiptHandleException(): ReceiptHandleIsInvalidException {
        return ReceiptHandleIsInvalidException("The input receipt handle is invalid").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "ReceiptHandleIsInvalid"
            statusCode = 404
            requestId = UUID.randomUUID().toString()
        }
    }

    fun createInvalidReceiptHandleForQueueException(handle: String): ReceiptHandleIsInvalidException {
        return ReceiptHandleIsInvalidException("The receipt handle \"$handle\" is not valid for this queue").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "ReceiptHandleIsInvalid"
            statusCode = 404
            requestId = UUID.randomUUID().toString()
        }
    }

    fun createInvalidVisibilityTimeoutException(value: Int): AmazonSQSException {
        return AmazonSQSException("Value $value for parameter VisibilityTimeout is invalid. Reason: VisibilityTimeout must be an integer between 0 and 43200").apply {
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "InvalidParameterValue"
            statusCode = 400
            requestId = UUID.randomUUID().toString()
        }
    }

    fun AmazonSQSException.toBatchResultErrorEntry(id: String): BatchResultErrorEntry = BatchResultErrorEntry()
            .withId(id)
            .withCode(errorCode)
            .withMessage(errorMessage)
            .withSenderFault(errorType == AmazonServiceException.ErrorType.Client)
}