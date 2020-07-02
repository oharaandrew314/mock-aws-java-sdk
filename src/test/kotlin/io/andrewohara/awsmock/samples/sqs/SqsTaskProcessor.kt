package io.andrewohara.awsmock.samples.sqs

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import java.time.Duration

class SqsTaskProcessor(private val cat: Cat, private val queue: String, sqs: AmazonSQS? = null) {

    companion object {
        private val pettingSessionTime = Duration.ofMinutes(10)
    }

    private val sqsClient = sqs ?: AmazonSQSClientBuilder.defaultClient()

    fun processNextTask() {
        for (message in sqsClient.receiveMessage(queue).messages) {
            when (message.body.toLowerCase()) {
                "trill" -> handleTrill()
                "meow" -> handleMeow()
                "purr" -> {
                    sqsClient.changeMessageVisibility(queue, message.receiptHandle, pettingSessionTime.seconds.toInt())
                    handlePurr()
                }
            }
            sqsClient.deleteMessage(queue, message.receiptHandle)
        }
    }

    private fun handleTrill() {
        cat.actions.add("I respond with Boss Nass' Head Shake")
    }

    private fun handleMeow() {
        cat.actions.add("I tell her it's not dinner time yet")
    }

    private fun handlePurr() {
        cat.actions.add("I give in and admit it's petting time")
    }
}

data class Cat(val actions: MutableList<String> = mutableListOf())