package io.andrewohara.awsmock.sns

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.*

class MockSnsV2(private val backend: MockSnsBackend = MockSnsBackend()): SnsClient {

    override fun close() {}
    override fun serviceName() = "sns-mock"

    override fun createTopic(request: CreateTopicRequest): CreateTopicResponse {
        val name = request.name() ?: throw SnsException.builder().message("name must not be null").build()
        val topic = backend.createTopic(name)

        return CreateTopicResponse.builder()
            .topicArn(topic.arn)
            .build()
    }

    override fun deleteTopic(request: DeleteTopicRequest): DeleteTopicResponse {
        val arn = request.topicArn() ?: throw SnsException.builder().message("arn must not be null").build()

        backend.deleteTopic(arn)

        return DeleteTopicResponse.builder()
            .build()
    }

    override fun listTopics(request: ListTopicsRequest): ListTopicsResponse {
        val topics = backend.topics().map { Topic.builder().topicArn(it.arn).build() }

        return ListTopicsResponse.builder()
            .topics(topics)
            .build()
    }

    override fun publish(request: PublishRequest): PublishResponse {
        val arn = request.topicArn() ?: throw SnsException.builder().message("arn must not be null").build()
        val message = request.message() ?: throw SnsException.builder().message("message must not be null").build()

        val topic = backend[arn] ?: throw NotFoundException.builder().message("Topic does not exist").build()

        val receipt = topic.publish(subject = request.subject(), message = message)

        return PublishResponse.builder()
            .messageId(receipt.messageId)
            .build()
    }
}