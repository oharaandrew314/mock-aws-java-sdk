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
import java.time.Duration
import java.util.*

class MockAmazonSQS: AbstractAmazonSQS() {

    companion object {
        private val validVisibilityTimeouts = 0..43200
    }

    private val queues = mutableListOf<MockQueue>()
    private val receipts = mutableMapOf<String, MockMessage>()

    // helpers

    operator fun get(url: String) = queues.firstOrNull { it.url.toString() == url }
    private fun getByName(name: String) = queues.firstOrNull { it.name == name }

    // Create Queue

    override fun createQueue(queueName: String): CreateQueueResult {
        val request = CreateQueueRequest(queueName)
        return createQueue(request)
    }

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

    override fun deleteQueue(queueUrl: String): DeleteQueueResult {
        val request = DeleteQueueRequest().withQueueUrl(queueUrl)

        return deleteQueue(request)
    }

    override fun deleteQueue(request: DeleteQueueRequest): DeleteQueueResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        queues.remove(queue)

        return DeleteQueueResult()
    }

    // List Queues

    override fun listQueues(): ListQueuesResult {
        val request = ListQueuesRequest()
        return listQueues(request)
    }

    override fun listQueues(queueNamePrefix: String): ListQueuesResult {
        val request = ListQueuesRequest().withQueueNamePrefix(queueNamePrefix)
        return listQueues(request)
    }

    override fun listQueues(request: ListQueuesRequest): ListQueuesResult {
        val urls = queues
                .filter { request.queueNamePrefix == null || it.name.startsWith(request.queueNamePrefix) }
                .map { it.url.toString() }

        return ListQueuesResult().withQueueUrls(urls)
    }

    // Get Queue URL

    override fun getQueueUrl(queueName: String): GetQueueUrlResult {
        val request = GetQueueUrlRequest()
                .withQueueName(queueName)

        return getQueueUrl(request)
    }

    override fun getQueueUrl(request: GetQueueUrlRequest): GetQueueUrlResult {
        val queue = getByName(request.queueName) ?: throw createQueueDoesNotExistException()

        return GetQueueUrlResult().withQueueUrl(queue.url.toString())
    }

    // Send Message

    override fun sendMessage(request: SendMessageRequest): SendMessageResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val message = MockMessage(
                id = UUID.randomUUID().toString(),
                body = request.messageBody,
                delay = request.delaySeconds?.let { Duration.ofSeconds(it.toLong()) } ?: Duration.ZERO
        )

        queue.messages.offer(message)

        return SendMessageResult().withMessageId(message.id)
    }

    override fun sendMessage(queueUrl: String, messageBody: String): SendMessageResult {
        val request = SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)

        return sendMessage(request)
    }

    // Send Message Batch

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

    override fun sendMessageBatch(queueUrl: String, entries: List<SendMessageBatchRequestEntry>): SendMessageBatchResult {
        val request = SendMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(entries)

        return sendMessageBatch(request)
    }

    // Receive Message

    override fun receiveMessage(queueUrl: String): ReceiveMessageResult {
        val request = ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(1)

        return receiveMessage(request)
    }

    override fun receiveMessage(request: ReceiveMessageRequest): ReceiveMessageResult {
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val messages = queue.messages
                .filter { it !in receipts.values }
                .take(request.maxNumberOfMessages)
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

    // Delete Message

    override fun deleteMessage(request: DeleteMessageRequest): DeleteMessageResult {
        val message = receipts[request.receiptHandle] ?: throw createInvalidReceiptHandleException()
        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        queue.messages.remove(message)
        queue.deleted.add(message)

        return DeleteMessageResult()
    }

    override fun deleteMessage(queueUrl: String, receiptHandle: String): DeleteMessageResult {
        val request = DeleteMessageRequest()
                .withQueueUrl(queueUrl)
                .withReceiptHandle(receiptHandle)

        return deleteMessage(request)
    }

    // Delete Message Batch

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

    override fun deleteMessageBatch(queueUrl: String, entries: List<DeleteMessageBatchRequestEntry>): DeleteMessageBatchResult {
        val request = DeleteMessageBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(entries)

        return deleteMessageBatch(request)
    }

    // Change Message Visibility

    override fun changeMessageVisibility(request: ChangeMessageVisibilityRequest): ChangeMessageVisibilityResult {
        if (request.visibilityTimeout !in validVisibilityTimeouts) throw createInvalidVisibilityTimeoutException(request.visibilityTimeout)

        val queue = get(request.queueUrl) ?: throw createQueueDoesNotExistException()

        val message = receipts[request.receiptHandle] ?: throw createInvalidReceiptHandleException()

        if (message !in (queue.messages + queue.deleted)) throw createInvalidReceiptHandleForQueueException(request.receiptHandle)

        message.delay = Duration.ofSeconds(request.visibilityTimeout.toLong())

        return ChangeMessageVisibilityResult()
    }

    override fun changeMessageVisibility(queueUrl: String, receiptHandle: String, visibilityTimeout: Int): ChangeMessageVisibilityResult {
        val request = ChangeMessageVisibilityRequest()
                .withQueueUrl(queueUrl)
                .withReceiptHandle(receiptHandle)
                .withVisibilityTimeout(visibilityTimeout)

        return changeMessageVisibility(request)
    }

    // change message visibility batch

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
                    message.delay = Duration.ofSeconds(entry.visibilityTimeout.toLong())
                    successes.add(ChangeMessageVisibilityBatchResultEntry().withId(entry.id))
                }
            }
        }

        return ChangeMessageVisibilityBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }

    override fun changeMessageVisibilityBatch(queueUrl: String, entries: List<ChangeMessageVisibilityBatchRequestEntry>): ChangeMessageVisibilityBatchResult {
        val request = ChangeMessageVisibilityBatchRequest()
                .withQueueUrl(queueUrl)
                .withEntries(entries)

        return changeMessageVisibilityBatch(request)
    }
}