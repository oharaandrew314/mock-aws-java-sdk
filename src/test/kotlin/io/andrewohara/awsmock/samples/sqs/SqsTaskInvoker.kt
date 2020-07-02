package io.andrewohara.awsmock.samples.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder

class SqsTaskInvoker(private val queue: String, sqs: AmazonSQS? = null) {

    private val sqsClient = sqs ?: AmazonSQSClientBuilder.defaultClient()

    fun meow() {
        sqsClient.sendMessage(queue, "meow")
    }

    fun trill() {
        sqsClient.sendMessage(queue, "trill")
    }

    fun purr() {
        sqsClient.sendMessage(queue, "purr")
    }
}