package io.andrewohara.awsmock.sns

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
}

class MockSnsTopic(
    val arn: String,
    val name: String
) {
    private val messages = mutableListOf<MockSnsMessage>()
    fun messages() = messages.toList()

    fun publish(message: String, subject: String?, attributes: Map<String, String>?) = MockSnsMessage(
        messageId = "$name:${messages.size}",
        subject = subject,
        message = message,
        attributes = attributes ?: emptyMap()
    ).also { messages += it }
}

data class MockSnsMessage(
    val messageId: String,
    val subject: String?,
    val message: String,
    val attributes: Map<String, String> = emptyMap()
)