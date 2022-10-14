package io.andrewohara.awsmock.sns

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sns.AbstractAmazonSNS
import com.amazonaws.services.sns.model.*
import java.util.*

class MockSnsV1(private val backend: MockSnsBackend = MockSnsBackend()): AbstractAmazonSNS() {

    override fun createTopic(request: CreateTopicRequest): CreateTopicResult {
        if (request.name == null) throw createValidationException("name")

        val topic = backend.createTopic(request.name)

        return CreateTopicResult().withTopicArn(topic.arn)
    }

    override fun deleteTopic(request: DeleteTopicRequest): DeleteTopicResult {
        if (request.topicArn == null) throw createInvalidParameterException("TopicArn")

        backend.deleteTopic(request.topicArn)

        return DeleteTopicResult()
    }

    override fun listTopics(request: ListTopicsRequest): ListTopicsResult {
        val topics = backend.topics()
            .map { Topic().withTopicArn(it.arn) }

        return ListTopicsResult()
            .withTopics(topics)
    }

    override fun publish(request: PublishRequest): PublishResult {
        if (request.topicArn == null) throw createInvalidParameterException("TopicArn or TargetArn")
        if (request.message == null) throw createValidationException("message")

        val topic = backend[request.topicArn] ?: throw NotFoundException("Topic does not exist").apply {
            requestId = UUID.randomUUID().toString()
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "NotFound"
            statusCode = 404
        }

        val attributes = request.messageAttributes?.mapValues { it.value.stringValue }
        val message = topic.publish(message = request.message, subject = request.subject, attributes = attributes)

        return PublishResult()
                .withMessageId(message.messageId)
    }

    override fun publishBatch(request: PublishBatchRequest): PublishBatchResult {
        if (request.topicArn == null) throw createInvalidParameterException("TopicArn or TargetArn")
        val topic = backend[request.topicArn] ?: throw NotFoundException("Topic does not exist").apply {
            requestId = UUID.randomUUID().toString()
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "NotFound"
            statusCode = 404
        }

        val receipts = request.publishBatchRequestEntries
            .withIndex()
            .map { (index, entry) ->
                val attributes = entry.messageAttributes?.mapValues { it.value.stringValue }
                val receipt = topic.publish(message = entry.message, subject = entry.subject, attributes = attributes)
                PublishBatchResultEntry()
                    .withId(entry.id)
                    .withMessageId(receipt.messageId)
                    .withSequenceNumber(index.toString())
            }

        return PublishBatchResult().withSuccessful(receipts)
    }

    private fun createInvalidParameterException(parameter: String) = InvalidParameterException("Invalid parameter: $parameter Reason: no value for required parameter").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "InvalidParameter"
        statusCode = 400
    }

    private fun createValidationException(parameter: String) = AmazonSNSException("1 validation error detected: Value null at '$parameter' failed to satisfy constraint: Member must not be null").apply {
        requestId = UUID.randomUUID().toString()
        errorType = AmazonServiceException.ErrorType.Client
        errorCode = "ValidationError"
        statusCode = 400
    }
}