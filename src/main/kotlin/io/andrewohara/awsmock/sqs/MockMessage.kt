package io.andrewohara.awsmock.sqs

data class MockMessage(
        val id: String,
        val body: String
)