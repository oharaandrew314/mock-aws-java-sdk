package io.andrewohara.awsmock.sqs

import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.backend.MockSqsQueue
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*
import java.time.Duration

class MockSqsV2(private val backend: MockSqsBackend = MockSqsBackend()): SqsClient {

    override fun close() {}
    override fun serviceName() = "sqs-mock"

    override fun createQueue(createQueueRequest: CreateQueueRequest): CreateQueueResponse {
        val queue = backend.create(createQueueRequest.queueName(), createQueueRequest.attributesAsStrings())
            ?: throw QueueNameExistsException.builder().message("queue already exists: ${createQueueRequest.queueName()}").build()

        return CreateQueueResponse.builder()
            .queueUrl(queue.url)
            .build()
    }

    override fun deleteQueue(deleteQueueRequest: DeleteQueueRequest): DeleteQueueResponse {
        if (!backend.delete(deleteQueueRequest.queueUrl())) throw queueDoesNotExist()

        return DeleteQueueResponse.builder().build()
    }

    override fun listQueues(listQueuesRequest: ListQueuesRequest): ListQueuesResponse {
        val urls = backend.queues(listQueuesRequest.queueNamePrefix(), listQueuesRequest.maxResults())
            .map { it.url }

        return ListQueuesResponse.builder()
            .queueUrls(urls)
            .build()
    }

    override fun getQueueUrl(getQueueUrlRequest: GetQueueUrlRequest): GetQueueUrlResponse {
        val queue = backend.queues().find { it.name == getQueueUrlRequest.queueName() } ?: throw queueDoesNotExist()

        return GetQueueUrlResponse.builder()
            .queueUrl(queue.url)
            .build()
    }

    override fun sendMessage(sendMessageRequest: SendMessageRequest): SendMessageResponse {
        val queue = backend[sendMessageRequest.queueUrl()] ?: throw queueDoesNotExist()

        val message = queue.send(
            body = sendMessageRequest.messageBody(),
            delay = sendMessageRequest.delaySeconds()?.seconds()
        )

        return SendMessageResponse.builder()
            .messageId(message.id)
            .build()
    }

    override fun sendMessageBatch(sendMessageBatchRequest: SendMessageBatchRequest): SendMessageBatchResponse {
        val queue = backend[sendMessageBatchRequest.queueUrl()] ?: throw queueDoesNotExist()

        val results = sendMessageBatchRequest.entries().map { entry ->
            val message = queue.send(
                body = entry.messageBody(),
                delay = entry.delaySeconds()?.seconds()
            )

            SendMessageBatchResultEntry.builder()
                .id(entry.id())
                .messageId(message.id)
                .build()
        }

        return SendMessageBatchResponse.builder()
            .successful(results)
            .build()
    }

    override fun receiveMessage(receiveMessageRequest: ReceiveMessageRequest): ReceiveMessageResponse {
        val queue = backend[receiveMessageRequest.queueUrl()] ?: throw queueDoesNotExist()

        val messages = queue.receive(receiveMessageRequest.maxNumberOfMessages() ?: 1, receiveMessageRequest.visibilityTimeout()?.seconds())
            .map { (receiptHandle, message) ->
                Message.builder()
                    .body(message.body)
                    .messageId(message.id)
                    .receiptHandle(receiptHandle)
                    .build()
            }

        return ReceiveMessageResponse.builder()
            .messages(messages)
            .build()
    }

    override fun deleteMessage(deleteMessageRequest: DeleteMessageRequest): DeleteMessageResponse {
        val queue = backend[deleteMessageRequest.queueUrl()] ?: throw queueDoesNotExist()

        if (!queue.delete(deleteMessageRequest.receiptHandle())) throw receiptHandleInvalid(deleteMessageRequest.receiptHandle())

        return DeleteMessageResponse.builder()
            .build()
    }

    override fun deleteMessageBatch(deleteMessageBatchRequest: DeleteMessageBatchRequest): DeleteMessageBatchResponse {
        val queue = backend[deleteMessageBatchRequest.queueUrl()] ?: throw queueDoesNotExist()

        val successes = mutableListOf<DeleteMessageBatchResultEntry>()
        val errors = mutableListOf<BatchResultErrorEntry>()

        for (entry in deleteMessageBatchRequest.entries()) {
            if (queue.delete(entry.receiptHandle())) {
                successes += DeleteMessageBatchResultEntry.builder().id(entry.id()).build()
            } else {
                errors += BatchResultErrorEntry.builder().id(entry.id()).message("receiptHandle not found: ${entry.receiptHandle()}").build()
            }
        }

        return DeleteMessageBatchResponse.builder()
            .successful(successes)
            .failed(errors)
            .build()
    }

    override fun changeMessageVisibility(request: ChangeMessageVisibilityRequest): ChangeMessageVisibilityResponse {
        val queue = backend[request.queueUrl()] ?: throw queueDoesNotExist()

        when(queue.updateVisibilityTimeout(request.receiptHandle(), request.visibilityTimeout().seconds())) {
            MockSqsQueue.UpdateVisibilityResult.Updated -> {}
            MockSqsQueue.UpdateVisibilityResult.InvalidTimeout -> throw SqsException.builder().message("visibility timeout is invalid: ${request.visibilityTimeout()}").build()
            MockSqsQueue.UpdateVisibilityResult.NotFound -> throw receiptHandleInvalid(request.receiptHandle())
        }

        return ChangeMessageVisibilityResponse.builder()
            .build()
    }

    override fun changeMessageVisibilityBatch(request: ChangeMessageVisibilityBatchRequest): ChangeMessageVisibilityBatchResponse {
        val queue = backend[request.queueUrl()] ?: throw queueDoesNotExist()

        val oks = mutableListOf<ChangeMessageVisibilityBatchResultEntry>()
        val fails = mutableListOf<BatchResultErrorEntry>()

        for (entry in request.entries()) {
            when(queue.updateVisibilityTimeout(entry.receiptHandle(), entry.visibilityTimeout().seconds())) {
                MockSqsQueue.UpdateVisibilityResult.NotFound -> fails += BatchResultErrorEntry.builder().id(entry.id()).message("receipt handle invalid: ${entry.receiptHandle()}").build()
                MockSqsQueue.UpdateVisibilityResult.Updated -> oks += ChangeMessageVisibilityBatchResultEntry.builder().id(entry.id()).build()
                MockSqsQueue.UpdateVisibilityResult.InvalidTimeout -> fails += BatchResultErrorEntry.builder().id(entry.id()).message("invalid visibility timeout: ${entry.visibilityTimeout()}").build()
            }
        }

        return ChangeMessageVisibilityBatchResponse.builder()
            .successful(oks)
            .failed(fails)
            .build()
    }

    private fun queueDoesNotExist() = QueueDoesNotExistException.builder()
        .message("queue does not exist")
        .build()

    private fun receiptHandleInvalid(receiptHandle: String) = ReceiptHandleIsInvalidException.builder()
        .message("receipt handle invalid: $receiptHandle")
        .build()

    private fun Int.seconds() = Duration.ofSeconds(toLong())
}