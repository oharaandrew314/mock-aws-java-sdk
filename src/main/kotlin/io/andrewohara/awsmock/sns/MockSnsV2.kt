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
        val attributes = request.messageAttributes()?.mapValues { it.value.stringValue() }

        val topic = backend[arn] ?: throw NotFoundException.builder().message("Topic does not exist").build()

        val receipt = topic.publish(subject = request.subject(), message = message, attributes = attributes)

        return PublishResponse.builder()
            .messageId(receipt.messageId)
            .build()
    }

    override fun publishBatch(request: PublishBatchRequest): PublishBatchResponse {
        val arn = request.topicArn() ?: throw SnsException.builder().message("arn must not be null").build()
        val topic = backend[arn] ?: throw NotFoundException.builder().message("Topic does not exist").build()
        if (request.publishBatchRequestEntries().orEmpty().size > MockSnsBackend.BATCH_SIZE_LIMIT) {
            throw SnsException.builder().message("batch size limit of ${MockSnsBackend.BATCH_SIZE_LIMIT}").build()
        }

        val receipts = request.publishBatchRequestEntries()
            .withIndex()
            .map { (index, entry) ->
                val attributes = entry.messageAttributes()?.mapValues { it.value.stringValue() }
                val receipt = topic.publish(subject = entry.subject(), message = entry.message(), attributes = attributes)
                PublishBatchResultEntry.builder()
                    .id(entry.id())
                    .messageId(receipt.messageId)
                    .sequenceNumber(index.toString())
                    .build()
            }

        return PublishBatchResponse.builder()
            .successful(receipts)
            .build()
    }
}