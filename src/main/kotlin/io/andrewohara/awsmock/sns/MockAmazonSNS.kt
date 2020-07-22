package io.andrewohara.awsmock.sns

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.sns.AbstractAmazonSNS
import com.amazonaws.services.sns.model.*
import java.util.*

class MockAmazonSNS: AbstractAmazonSNS() {

    private val topics = mutableSetOf<Topic>()

    override fun createTopic(request: CreateTopicRequest): CreateTopicResult {
        if (request.name == null) throw createValidationException("name")

        val topic = Topic().withTopicArn("arn:mockaws:sns:region:account-id:${request.name}")
        topics.add(topic)

        return CreateTopicResult().withTopicArn(topic.topicArn)
    }

    override fun deleteTopic(request: DeleteTopicRequest): DeleteTopicResult {
        if (request.topicArn == null) throw createInvalidParameterException("TopicArn")

        topics.removeIf { it.topicArn == request.topicArn }

        return DeleteTopicResult()
    }

    override fun listTopics(request: ListTopicsRequest): ListTopicsResult {
        return ListTopicsResult().withTopics(topics)
    }

    override fun publish(request: PublishRequest): PublishResult {
        if (request.topicArn == null) throw createInvalidParameterException("TopicArn or TargetArn")
        if (request.message == null) throw createValidationException("message")

        topics.firstOrNull { it.topicArn == request.topicArn } ?: throw NotFoundException("Topic does not exist").apply {
            requestId = UUID.randomUUID().toString()
            errorType = AmazonServiceException.ErrorType.Client
            errorCode = "NotFound"
            statusCode = 404
        }

        return PublishResult()
                .withMessageId(UUID.randomUUID().toString())
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