package io.andrewohara.awsmock.samples.sqs

import com.amazonaws.services.sqs.AmazonSQS

class CatSpa(private val sqs: AmazonSQS, private val groomingQueueUrl: String) {

    fun startAppointment(catName: String) {
        sqs.sendMessage(groomingQueueUrl, catName)
    }
}