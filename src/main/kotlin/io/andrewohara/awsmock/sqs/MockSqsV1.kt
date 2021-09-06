package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.AbstractAmazonSQS
import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.SQSExceptions.createEmptyBatchException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidParameterException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidReceiptHandleException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidVisibilityTimeoutException
import io.andrewohara.awsmock.sqs.SQSExceptions.createQueueDoesNotExistException
import io.andrewohara.awsmock.sqs.SQSExceptions.createQueueExistsException
import io.andrewohara.awsmock.sqs.SQSExceptions.toBatchResultErrorEntry
import io.andrewohara.awsmock.sqs.backend.MockSqsBackend
import io.andrewohara.awsmock.sqs.backend.MockSqsQueue
import java.time.Duration

class MockSqsV1(private val backend: MockSqsBackend = MockSqsBackend()): AbstractAmazonSQS() {

    override fun createQueue(request: CreateQueueRequest): CreateQueueResult {
        if (request.queueName == null) throw createInvalidParameterException()
        val queue = backend.create(request.queueName, request.attributes) ?: throw createQueueExistsException()

        return CreateQueueResult().withQueueUrl(queue.url)
    }

    override fun deleteQueue(request: DeleteQueueRequest): DeleteQueueResult {
        if (backend.delete(request.queueUrl)) {
            return DeleteQueueResult()
        }

        throw createQueueDoesNotExistException()
    }

    override fun listQueues(request: ListQueuesRequest): ListQueuesResult {
        val urls = backend.queues(request.queueNamePrefix)
            .map { it.url }

        return ListQueuesResult().withQueueUrls(urls)
    }

    override fun getQueueUrl(request: GetQueueUrlRequest): GetQueueUrlResult {
        val queue = backend.queues().find { it.name == request.queueName } ?: throw createQueueDoesNotExistException()

        return GetQueueUrlResult().withQueueUrl(queue.url)
    }

    override fun sendMessage(request: SendMessageRequest): SendMessageResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()
        val message = queue.send(
            request.messageBody,
            request.delaySeconds?.seconds()
        )

        return SendMessageResult().withMessageId(message.id)
    }

    override fun sendMessageBatch(request: SendMessageBatchRequest): SendMessageBatchResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()
        if (request.entries.isEmpty()) throw createEmptyBatchException(SendMessageBatchRequestEntry::class.java)

        val entryResults = request.entries
                .map { entry ->
                    val message = queue.send(
                        entry.messageBody,
                        entry.delaySeconds?.seconds()
                    )

                    SendMessageBatchResultEntry()
                            .withId(entry.id)
                            .withMessageId(message.id)
                }

        return SendMessageBatchResult().withSuccessful(entryResults)
    }

    override fun receiveMessage(request: ReceiveMessageRequest): ReceiveMessageResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        val messages = queue.receive(request.maxNumberOfMessages ?: 1, request.visibilityTimeout?.seconds())
            .map { (receiptHandle, message) ->
                Message()
                    .withBody(message.body)
                    .withMessageId(message.id)
                    .withReceiptHandle(receiptHandle)
            }

        return ReceiveMessageResult().withMessages(messages)
    }

    override fun deleteMessage(request: DeleteMessageRequest): DeleteMessageResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        if (!queue.delete(request.receiptHandle)) throw createInvalidReceiptHandleException()

        return DeleteMessageResult()
    }

    override fun deleteMessageBatch(request: DeleteMessageBatchRequest): DeleteMessageBatchResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        val failures = mutableListOf<BatchResultErrorEntry>()
        val successes = mutableListOf<DeleteMessageBatchResultEntry>()

        for (entry in request.entries) {
            if (queue.delete(entry.receiptHandle)) {
                successes += DeleteMessageBatchResultEntry().withId(entry.id)
            } else {
                failures += createInvalidReceiptHandleException().toBatchResultErrorEntry(entry.id)
            }
        }

        return DeleteMessageBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }

    override fun changeMessageVisibility(request: ChangeMessageVisibilityRequest): ChangeMessageVisibilityResult {
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        return when (queue.updateVisibilityTimeout(request.receiptHandle, request.visibilityTimeout.seconds())) {
            MockSqsQueue.UpdateVisibilityResult.Updated -> ChangeMessageVisibilityResult()
            MockSqsQueue.UpdateVisibilityResult.NotFound -> throw createInvalidReceiptHandleException()
            MockSqsQueue.UpdateVisibilityResult.InvalidTimeout -> throw createInvalidVisibilityTimeoutException(request.visibilityTimeout)
        }
    }

    override fun changeMessageVisibilityBatch(request: ChangeMessageVisibilityBatchRequest): ChangeMessageVisibilityBatchResult {
        if (request.entries.isEmpty()) throw createEmptyBatchException(ChangeMessageVisibilityBatchRequestEntry::class.java)
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        val successes = mutableListOf<ChangeMessageVisibilityBatchResultEntry>()
        val failures = mutableListOf<BatchResultErrorEntry>()

        for (entry in request.entries) {
            when (queue.updateVisibilityTimeout(entry.receiptHandle, entry.visibilityTimeout.seconds())) {
                MockSqsQueue.UpdateVisibilityResult.Updated -> successes += ChangeMessageVisibilityBatchResultEntry().withId(entry.id)
                MockSqsQueue.UpdateVisibilityResult.NotFound -> failures += createInvalidReceiptHandleException().toBatchResultErrorEntry(entry.id)
                MockSqsQueue.UpdateVisibilityResult.InvalidTimeout -> failures += createInvalidVisibilityTimeoutException(entry.visibilityTimeout).toBatchResultErrorEntry(entry.id)
            }
        }

        return ChangeMessageVisibilityBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }

    private fun Int.seconds() = Duration.ofSeconds(toLong())
}