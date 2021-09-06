package io.andrewohara.awsmock.samples.sqs

import software.amazon.awssdk.services.sqs.SqsClient

class CatSpa(private val sqs: SqsClient, private val groomingQueueUrl: String) {

    fun startAppointment(catName: String) {
        sqs.sendMessage {
            it.queueUrl(groomingQueueUrl)
            it.messageBody(catName)
        }
    }
}