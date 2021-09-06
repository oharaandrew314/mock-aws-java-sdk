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
import java.time.Duration

class MockSqsV1(private val backend: MockSqsBackend = MockSqsBackend()): AbstractAmazonSQS() {

    companion object {
        private val validVisibilityTimeouts = 0..43200
    }

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
            request.delaySeconds?.seconds(),
            request.messageAttributes.mapValues { it.value.toMock() }
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
                        entry.delaySeconds?.seconds(),
                        entry.messageAttributes.mapValues { it.value.toMock() }
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
        if (request.visibilityTimeout !in validVisibilityTimeouts) throw createInvalidVisibilityTimeoutException(request.visibilityTimeout)
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        return when (queue.updateVisibilityTimeout(request.receiptHandle, request.visibilityTimeout.seconds())) {
            MockSqsUpdateVisibilityResult.Updated -> ChangeMessageVisibilityResult()
            MockSqsUpdateVisibilityResult.NotFound -> throw createInvalidReceiptHandleException()
            MockSqsUpdateVisibilityResult.InvalidTimeout -> throw createInvalidVisibilityTimeoutException(request.visibilityTimeout)
        }
    }

    override fun changeMessageVisibilityBatch(request: ChangeMessageVisibilityBatchRequest): ChangeMessageVisibilityBatchResult {
        if (request.entries.isEmpty()) throw createEmptyBatchException(ChangeMessageVisibilityBatchRequestEntry::class.java)
        val queue = backend[request.queueUrl] ?: throw createQueueDoesNotExistException()

        val successes = mutableListOf<ChangeMessageVisibilityBatchResultEntry>()
        val failures = mutableListOf<BatchResultErrorEntry>()

        for (entry in request.entries) {
            when (queue.updateVisibilityTimeout(entry.receiptHandle, entry.visibilityTimeout.seconds())) {
                MockSqsUpdateVisibilityResult.Updated -> successes += ChangeMessageVisibilityBatchResultEntry().withId(entry.id)
                MockSqsUpdateVisibilityResult.NotFound -> failures += createInvalidReceiptHandleException().toBatchResultErrorEntry(entry.id)
                MockSqsUpdateVisibilityResult.InvalidTimeout -> failures += createInvalidVisibilityTimeoutException(entry.visibilityTimeout).toBatchResultErrorEntry(entry.id)
            }
        }

        return ChangeMessageVisibilityBatchResult()
                .withFailed(failures)
                .withSuccessful(successes)
    }

    private fun MessageAttributeValue.toMock() = when(dataType) {
        "String" -> if (stringListValues != null) MockSqsAttribute.TextList(stringListValues) else MockSqsAttribute.Text(stringValue)
        "Number" -> if (stringListValues != null) MockSqsAttribute.NumberList(stringListValues.map { it.toLong() }) else MockSqsAttribute.Number(stringValue.toLong())
        "Binary" -> if (binaryListValues != null) MockSqsAttribute.BinaryList(binaryListValues) else MockSqsAttribute.Binary(binaryValue)
        else -> TODO()
    }

    private fun Int.seconds() = Duration.ofSeconds(toLong())
}