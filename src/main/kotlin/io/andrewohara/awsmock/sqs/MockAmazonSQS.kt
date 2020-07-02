package io.andrewohara.awsmock.sqs

import com.amazonaws.services.sqs.AbstractAmazonSQS
import com.amazonaws.services.sqs.model.*
import io.andrewohara.awsmock.sqs.SQSExceptions.createEmptyBatchException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidParameterException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidReceiptHandleException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidReceiptHandleForQueueException
import io.andrewohara.awsmock.sqs.SQSExceptions.createInvalidVisibilityTimeoutException
import io.andrewohara.awsmock.sqs.SQSExceptions.createQueueDoesNotExistException
import io.andrewohara.awsmock.sqs.SQSExceptions.createQueueExistsException
import io.andrewohara.awsmock.sqs.SQSExceptions.toBatchResultErrorEntry
import java.net.URL
import java.util.*

class MockAmazonSQS: AbstractAmazonSQS() {

    companion object {
        private val validVisibilityTimeouts = 0..43200
    }

    private val queues = mutableListOf<MockQueue>()
    private val receipts = mutableMapOf<String, MockMessage>()

    // helpers

    operator fun get(url: String) = queues.firstOrNull { it.url.toString() == url }

    // Create Queue

    override fun createQueue(request: CreateQueueRequest): CreateQueueResult {
        if (request.queueName == null) throw createInvalidParameterException()

        queues.firstOrNull { it.name == request.queueName }?.let { existing ->
            if (request.attributes != existing.attributes) throw createQueueExistsException()

            return CreateQueueResult().withQueueUrl(existing.url.toString())
        }

        val queue = MockQueue(
                name = request.queueName,
                url = URL("https://sqs.mock.aws/${request.queueName}"),
                attributes = request.attributes.toMap()
        )
        queues.add(queue)

        return CreateQueueResult().withQueueUrl(queue.url.toString())
    }

    // Delete Queue

    override fun deleteQueue(request: DeleteQueueRequest): DeleteQueueResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        queues.remove(queue)

        return DeleteQueueResult()
    }

    override fun listQueues(request: ListQueuesRequest): ListQueuesResult {
        val urls = queues
                .filter { request.queueNamePrefix == null || it.name.startsWith(request.queueNamePrefix) }
                .map { it.url.toString() }

        return ListQueuesResult().withQueueUrls(urls)
    }

    override fun getQueueUrl(request: GetQueueUrlRequest): GetQueueUrlResult {
        val queue = queues.firstOrNull { it.name == request.queueName } ?: throw createQueueDoesNotExistException()

        return GetQueueUrlResult().withQueueUrl(queue.url.toString())
    }

    override fun sendMessage(request: SendMessageRequest): SendMessageResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val message = MockMessage(
                id = UUID.randomUUID().toString(),
                body = request.messageBody
        )

        queue.messages.offer(message)

        return SendMessageResult().withMessageId(message.id)
    }

    override fun sendMessageBatch(request: SendMessageBatchRequest): SendMessageBatchResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()
        if (request.entries.isEmpty()) throw createEmptyBatchException(SendMessageBatchRequestEntry::class.java)

        val entryResults = request.entries
                .map { entry ->
                    val entryRequest = SendMessageRequest()
                            .withMessageBody(entry.messageBody)
                            .withMessageAttributes(entry.messageAttributes)
                            .withDelaySeconds(entry.delaySeconds)
                            .withQueueUrl(queue.url.toString())

                    val result = sendMessage(entryRequest)

                    SendMessageBatchResultEntry()
                            .withId(entry.id)
                            .withMessageId(result.messageId)
                }

        return SendMessageBatchResult().withSuccessful(entryResults)
    }

    override fun receiveMessage(request: ReceiveMessageRequest): ReceiveMessageResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val messages = queue.messages
                .filter { it !in receipts.values }
                .take(request.maxNumberOfMessages ?: 1)
                .map { message ->
                    val receiptHandle = UUID.randomUUID().toString()
                    receipts[receiptHandle] = message
                    Message()
                            .withBody(message.body)
                            .withMessageId(message.id)
                            .withReceiptHandle(receiptHandle)
                }

        return ReceiveMessageResult().withMessages(messages)
    }

    override fun deleteMessage(request: DeleteMessageRequest): DeleteMessageResult {
        val message = receipts[request.receiptHandle] ?: throw createInvalidReceiptHandleException()
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        queue.messages.remove(message)
        queue.deleted.add(message)

        return DeleteMessageResult()
    }

    override fun deleteMessageBatch(request: DeleteMessageBatchRequest): DeleteMessageBatchResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val failures = mutableListOf<BatchResultErrorEntry>()
        val successes = mutableListOf<DeleteMessageBatchResultEntry>()

        for (entry in request.entries) {
            when (val message = receipts[entry.receiptHandle]) {
                null -> {
                    failures.add(createInvalidReceiptHandleException().toBatchResultErrorEntry(entry.id))
                }
                !in (queue.messages + queue.deleted) -> {
                    failures.add(createInvalidReceiptHandleForQueueException(entry.receiptHandle).toBatchResultErrorEntry(entry.id))
                }
                else -> {
                    successes.add(DeleteMessageBatchResultEntry().withId(entry.id))
                    queue.messages.remove(message)
                    queue.deleted.add(message)
                }
            }
        }

        return DeleteMessageBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }

    override fun changeMessageVisibility(request: ChangeMessageVisibilityRequest): ChangeMessageVisibilityResult {
        if (request.visibilityTimeout !in validVisibilityTimeouts) throw createInvalidVisibilityTimeoutException(request.visibilityTimeout)

        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val message = receipts[request.receiptHandle] ?: throw createInvalidReceiptHandleException()

        if (message !in (queue.messages + queue.deleted)) throw createInvalidReceiptHandleForQueueException(request.receiptHandle)

        // happy-path is actually a no-op because nothing here currently cares about the delay

        return ChangeMessageVisibilityResult()
    }

    override fun changeMessageVisibilityBatch(request: ChangeMessageVisibilityBatchRequest): ChangeMessageVisibilityBatchResult {
        if (request.entries.isEmpty()) throw createEmptyBatchException(ChangeMessageVisibilityBatchRequestEntry::class.java)
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val successes = mutableListOf<ChangeMessageVisibilityBatchResultEntry>()
        val failures = mutableListOf<BatchResultErrorEntry>()

        for (entry in request.entries) {
            val message = receipts[entry.receiptHandle]
            when {
                message == null -> failures.add(createInvalidReceiptHandleException().toBatchResultErrorEntry(entry.id))
                message !in (queue.messages + queue.deleted) -> failures.add(createInvalidReceiptHandleForQueueException(entry.receiptHandle).toBatchResultErrorEntry(entry.id))
                entry.visibilityTimeout !in validVisibilityTimeouts -> failures.add(createInvalidParameterException().toBatchResultErrorEntry(entry.id))
                else -> {
                    // happy-path is actually a no-op because nothing here currently cares about the delay
                    successes.add(ChangeMessageVisibilityBatchResultEntry().withId(entry.id))
                }
            }
        }

        return ChangeMessageVisibilityBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }
}