package io.andrewohara.awsmock.sns

import java.util.*

class MockSnsBackend {

    private val topics = mutableSetOf<MockSnsTopic>()
    fun topics() = topics.toSet()

    operator fun get(arn: String) = topics.find { it.arn == arn }

    fun createTopic(name: String): MockSnsTopic {
        topics.find { it.name == name }?.let { return it }

        return MockSnsTopic(
            arn = "arn:mockaws:sns:region:account-id:$name",
            name = name
        ).also { topics += it }
    }

    fun deleteTopic(arn: String) = topics.removeIf { it.arn == arn }

    fun clear() = topics.clear()
}

class MockSnsTopic(
    val arn: String,
    val name: String
) {
    private val messages = mutableListOf<MockSnsMessage>()
    fun messages() = messages.toList()

    fun publish(message: String, subject: String?) = MockSnsMessage(
        message = message,
        subject = subject,
        messageId = UUID.randomUUID().toString()
    ).also { messages += it }
}

data class MockSnsMessage(
    val subject: String?,
    val message: String,
    val messageId: String
)